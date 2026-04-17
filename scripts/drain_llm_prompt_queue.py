#!/usr/bin/env python3
"""
Drain llm_prompt_queue via backend admin API + OpenAI / Anthropic chat completions.

Backends supported:
  - OpenAI (OPENAI_API_KEY)         — original
  - Anthropic (ANTHROPIC_API_KEY)   — default quando presente
  LLM_BACKEND=auto|anthropic|openai (default auto: prefere Anthropic se ANTHROPIC_API_KEY, senao OpenAI)

Rate-limit awareness (cross-session):
  - Le /tmp/claude_rate_limit_until no start. Se timestamp futuro → exit 0 silencioso.
  - Em 429/insufficient_quota/usage_limit, escreve proximo 17:00 America/Sao_Paulo (20:00 UTC) e aborta.

Uses the same tier/phase → model routing as .cursor/skills/run-llm-cursor/SKILL.md.

Usage:
  set -a; source deploy/.env; set +a
  python3 scripts/drain_llm_prompt_queue.py

Environment (optional, defaults match backend LlmConfig / docker-compose):
  OPENAI_LOW_MODEL, OPENAI_MEDIUM_MODEL, OPENAI_HIGH_MODEL
  ANTHROPIC_LOW_MODEL=claude-haiku-4-5, ANTHROPIC_MEDIUM_MODEL=claude-sonnet-4-6, ANTHROPIC_HIGH_MODEL=claude-opus-4-7
  LLM_BACKEND=auto|anthropic|openai
  LLM_QUEUE_BASE_URL (default http://localhost:8080)
  LLM_QUEUE_EMAIL (default dev@manuscriptum.local)
  QUEUE_UNSTICK_MINUTES — default 10 (lease reaper built-in). Set 0 para desligar.
  DRAIN_MAX_BATCHES — stop after N claim/process/apply rounds (0 = unlimited)
  OPENAI_MAX_429_RETRIES, OPENAI_429_BACKOFF_SEC, OPENAI_HTTP_TIMEOUT
  RATE_LIMIT_FILE (default /tmp/claude_rate_limit_until)
"""

from __future__ import annotations

import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from typing import Any

BASE = os.environ.get("LLM_QUEUE_BASE_URL", "http://localhost:8080").rstrip("/")
EMAIL = os.environ.get("LLM_QUEUE_EMAIL", "dev@manuscriptum.local")


@dataclass
class DrainReport:
    phases_applied: set[str] = field(default_factory=set)
    failures: list[tuple[int, str, str]] = field(default_factory=list)  # id, phase, message
    batches: int = 0
    items_completed: int = 0


report = DrainReport()


def http_json(method: str, path: str, body: Any | None = None, token: str | None = None) -> Any:
    data = None if body is None else json.dumps(body).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(f"{BASE}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=600) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code} {path}: {body}") from e


def dev_login() -> str:
    r = http_json("POST", f"/auth/dev-login?email={urllib.parse.quote(EMAIL)}")
    return r["token"]


def get_stats(token: str) -> dict[str, Any]:
    return http_json("GET", "/admin/llm/queue/stats", token=token)


def claim(token: str, *, limit: int, tier: str | None = None, phase: str | None = None) -> list[dict[str, Any]]:
    q = f"limit={limit}"
    if tier:
        q += f"&tier={urllib.parse.quote(tier)}"
    if phase:
        q += f"&phase={urllib.parse.quote(phase)}"
    return http_json("POST", f"/admin/llm/queue/claim?{q}", token=token)


def complete_item(
    token: str,
    item_id: int,
    response: str,
    model_used: str,
    input_tokens: int,
    output_tokens: int,
) -> None:
    http_json(
        "POST",
        f"/admin/llm/queue/{item_id}/complete",
        body={
            "id": item_id,
            "responseContent": response,
            "modelUsed": model_used,
            "inputTokens": input_tokens,
            "outputTokens": output_tokens,
        },
        token=token,
    )


def fail_item(token: str, item_id: int, message: str) -> None:
    http_json("POST", f"/admin/llm/queue/{item_id}/fail", body={"message": message}, token=token)


def apply_phase(token: str, phase: str) -> str:
    r = http_json("POST", f"/admin/llm/queue/apply/{urllib.parse.quote(phase, safe='')}", token=token)
    return r.get("message", str(r))


def low_model() -> str:
    return os.environ.get("OPENAI_LOW_MODEL", "gpt-4.1-mini")


def medium_model() -> str:
    return os.environ.get("OPENAI_MEDIUM_MODEL", "gpt-5.4")


def high_model() -> str:
    return os.environ.get("OPENAI_HIGH_MODEL", "gpt-5.4")


def route_scope_and_model(item: dict[str, Any]) -> tuple[str, str]:
    """Returns (scope_label, openai_model_id)."""
    tier = item.get("tier") or ""
    phase = item.get("phaseName") or ""
    if tier == "LOW":
        return "LOW", low_model()
    if "bible_translate_enrichment_" in phase:
        return "MEDIUM_ENRICH", low_model()
    if tier == "HIGH":
        return "HIGH", high_model()
    if phase.startswith("bible_align_") or phase.startswith("dating_") or phase.startswith("apologetics_"):
        return "HIGH", high_model()
    if phase.startswith("DatingEnrichment:"):
        return "HIGH", high_model()
    if tier == "MEDIUM":
        return "MEDIUM_STRUCT", medium_model()
    return "MEDIUM_STRUCT", medium_model()


def parallelism_for_scope(scope: str) -> int:
    if scope == "LOW":
        return 8
    if scope == "MEDIUM_ENRICH":
        return 6
    if scope == "MEDIUM_STRUCT":
        return 4
    if scope == "HIGH":
        return 2
    return 4


def normalize_llm_text(text: str) -> str:
    s = text.strip()
    m = re.match(r"<response>\s*(.*)\s*</response>\s*$", s, re.DOTALL | re.IGNORECASE)
    if m:
        s = m.group(1).strip()
    if s.startswith("```"):
        s = re.sub(r"^```(?:json)?\s*", "", s)
        s = re.sub(r"\s*```\s*$", "", s)
    return s.strip()


def expects_json_object(system_prompt: str) -> bool:
    s = system_prompt.lower()
    return "json object" in s or "only the json" in s or "valid json" in s


def openai_chat(
    api_key: str,
    model: str,
    system: str,
    user: str,
    max_tokens: int,
    *,
    json_object: bool,
) -> tuple[str, int, int]:
    payload: dict[str, Any] = {
        "model": model,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "temperature": 0,
        "max_completion_tokens": min(max(256, max_tokens), 16384),
    }
    if json_object:
        payload["response_format"] = {"type": "json_object"}
    timeout = int(os.environ.get("OPENAI_HTTP_TIMEOUT", "300"))

    def _post(p: dict[str, Any]) -> dict[str, Any]:
        rq = urllib.request.Request(
            "https://api.openai.com/v1/chat/completions",
            data=json.dumps(p).encode("utf-8"),
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(rq, timeout=timeout) as resp:
            return json.loads(resp.read().decode("utf-8"))

    max_429 = int(os.environ.get("OPENAI_MAX_429_RETRIES", "8"))
    backoff = float(os.environ.get("OPENAI_429_BACKOFF_SEC", "3"))
    attempt = 0
    while True:
        try:
            r = _post(payload)
            break
        except urllib.error.HTTPError as e:
            err = e.read().decode("utf-8", errors="replace")
            if "insufficient_quota" in err:
                raise RuntimeError(f"OpenAI insufficient_quota: {err[:800]}") from e
            if e.code == 429:
                if attempt < max_429:
                    attempt += 1
                    ra = e.headers.get("Retry-After") if e.headers else None
                    wait = float(ra) if ra and ra.isdigit() else backoff * attempt
                    time.sleep(min(wait, 120.0))
                    continue
                raise RuntimeError(f"OpenAI HTTP 429 (exhausted retries): {err[:800]}") from e
            if json_object and ("response_format" in err.lower() or "json_object" in err.lower()):
                del payload["response_format"]
                r = _post(payload)
                break
            raise RuntimeError(f"OpenAI HTTP {e.code}: {err[:800]}") from e
    choice = r["choices"][0]
    msg = choice.get("message") or {}
    content = (msg.get("content") or "").strip()
    usage = r.get("usage") or {}
    inp = int(usage.get("prompt_tokens") or 0)
    out = int(usage.get("completion_tokens") or 0)
    return content, inp, out


class OpenAiQuotaExceeded(RuntimeError):
    pass


class AnthropicQuotaExceeded(RuntimeError):
    pass


RATE_LIMIT_FILE = os.environ.get("RATE_LIMIT_FILE", "/tmp/claude_rate_limit_until")


def _next_sp_reset_epoch() -> int:
    """Proximo 17:00 America/Sao_Paulo = 20:00 UTC."""
    import datetime
    now = datetime.datetime.now(datetime.timezone.utc)
    target = now.replace(hour=20, minute=0, second=0, microsecond=0)
    if target <= now:
        target += datetime.timedelta(days=1)
    return int(target.timestamp())


def _write_rate_limit_stop() -> None:
    try:
        with open(RATE_LIMIT_FILE, "w") as f:
            f.write(str(_next_sp_reset_epoch()))
    except Exception:
        pass


def _rate_limit_active() -> int:
    """Retorna epoch futuro se rate-limit ativo, 0 se nao."""
    try:
        raw = open(RATE_LIMIT_FILE).read().strip()
        if not raw:
            return 0
        until = int(raw)
        if until > int(time.time()):
            return until
    except Exception:
        pass
    return 0


def _jsonl(**kwargs) -> None:
    """Structured JSON log em uma linha para stdout."""
    print(json.dumps(kwargs, ensure_ascii=False), flush=True)


def anthropic_low_model() -> str:
    return os.environ.get("ANTHROPIC_LOW_MODEL", "claude-haiku-4-5")


def anthropic_medium_model() -> str:
    return os.environ.get("ANTHROPIC_MEDIUM_MODEL", "claude-sonnet-4-6")


def anthropic_high_model() -> str:
    return os.environ.get("ANTHROPIC_HIGH_MODEL", "claude-opus-4-7")


def anthropic_model_for_scope(scope: str) -> str:
    if scope == "LOW":
        return anthropic_low_model()
    if scope == "MEDIUM_ENRICH":
        return anthropic_low_model()  # enrichment = Haiku
    if scope == "HIGH":
        return anthropic_high_model()
    return anthropic_medium_model()


def anthropic_chat(
    api_key: str,
    model: str,
    system: str,
    user: str,
    max_tokens: int,
) -> tuple[str, int, int]:
    payload = {
        "model": model,
        "max_tokens": min(max(256, max_tokens), 16384),
        "temperature": 0,
        "system": system,
        "messages": [{"role": "user", "content": user}],
    }
    headers = {
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    timeout = int(os.environ.get("OPENAI_HTTP_TIMEOUT", "300"))
    max_429 = int(os.environ.get("OPENAI_MAX_429_RETRIES", "8"))
    backoff = float(os.environ.get("OPENAI_429_BACKOFF_SEC", "3"))
    attempt = 0
    while True:
        try:
            rq = urllib.request.Request(
                "https://api.anthropic.com/v1/messages",
                data=json.dumps(payload).encode("utf-8"),
                headers=headers,
                method="POST",
            )
            with urllib.request.urlopen(rq, timeout=timeout) as resp:
                r = json.loads(resp.read().decode("utf-8"))
            break
        except urllib.error.HTTPError as e:
            err = e.read().decode("utf-8", errors="replace")
            low = err.lower()
            if e.code == 401 or "invalid_api_key" in low:
                raise RuntimeError(f"Anthropic auth failure: {err[:400]}") from e
            if e.code in (429, 529) or "rate_limit" in low or "overloaded" in low or "quota" in low:
                if "quota" in low or "billing" in low:
                    raise AnthropicQuotaExceeded(f"Anthropic quota: {err[:400]}") from e
                if attempt < max_429:
                    attempt += 1
                    ra = e.headers.get("retry-after") if e.headers else None
                    wait = float(ra) if ra and ra.replace(".", "", 1).isdigit() else backoff * attempt
                    time.sleep(min(wait, 120.0))
                    continue
                raise RuntimeError(f"Anthropic HTTP {e.code} (exhausted retries): {err[:400]}") from e
            raise RuntimeError(f"Anthropic HTTP {e.code}: {err[:400]}") from e
    blocks = r.get("content") or []
    text = "".join(b.get("text", "") for b in blocks if b.get("type") == "text").strip()
    usage = r.get("usage") or {}
    inp = int(usage.get("input_tokens") or 0)
    out = int(usage.get("output_tokens") or 0)
    return text, inp, out


def process_one_item(token: str, api_key: str, item: dict[str, Any]) -> None:
    item_id = item["id"]
    phase = item["phaseName"]
    scope, openai_model = route_scope_and_model(item)
    system = item["systemPrompt"]
    user = item["userContent"]
    max_tok = int(item.get("maxTokens") or 1024)
    want_json = expects_json_object(system)
    backend = LLM_BACKEND
    try:
        if backend == "anthropic":
            model_used = anthropic_model_for_scope(scope)
            try:
                raw, inp, out = anthropic_chat(ANTHROPIC_API_KEY, model_used, system, user, max_tok)
            except AnthropicQuotaExceeded:
                raise
        else:
            model_used = openai_model
            try:
                raw, inp, out = openai_chat(
                    api_key, model_used, system, user, max_tok, json_object=want_json
                )
            except RuntimeError as rexc:
                if "insufficient_quota" in str(rexc):
                    raise OpenAiQuotaExceeded(str(rexc)) from rexc
                raise
        text = normalize_llm_text(raw)
        if not text:
            raise ValueError("empty model output")
        if os.environ.get("STRICT_JSON_VALIDATE") == "1" and want_json:
            json.loads(text)
        complete_item(token, item_id, text, model_used, inp, out)
        report.items_completed += 1
    except (OpenAiQuotaExceeded, AnthropicQuotaExceeded):
        raise
    except Exception as e:
        msg = f"{type(e).__name__}: {e}"
        try:
            fail_item(token, item_id, msg[:2000])
        except Exception as fe:
            report.failures.append((item_id, phase, f"{msg} (fail HTTP: {fe})"))
            return
        report.failures.append((item_id, phase, msg))


def unique_phases(batch: list[dict[str, Any]]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for it in batch:
        p = it["phaseName"]
        if p not in seen:
            seen.add(p)
            out.append(p)
    return out


# Fixed order: no stats round-trip needed to discover enrichment phases.
ENRICHMENT_PHASES: tuple[str, ...] = (
    "bible_translate_enrichment_greek",
    "bible_translate_enrichment_hebrew",
)


def batch_scope(batch: list[dict[str, Any]]) -> str:
    _, m = route_scope_and_model(batch[0])
    s, _ = route_scope_and_model(batch[0])
    return s


def run_batch_parallel(token: str, api_key: str, batch: list[dict[str, Any]]) -> None:
    if not batch:
        return
    scope = batch_scope(batch)
    workers = parallelism_for_scope(scope)
    workers = min(workers, len(batch))
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futs = [ex.submit(process_one_item, token, api_key, it) for it in batch]
        for fu in as_completed(futs):
            try:
                fu.result()
            except (OpenAiQuotaExceeded, AnthropicQuotaExceeded):
                for f2 in futs:
                    f2.cancel()
                raise


def try_claim_next(token: str, stats_cache: dict[str, Any]) -> tuple[list[dict[str, Any]], str | None]:
    """
    Priority: LOW → MEDIUM enrichment (by fixed phase order) → MEDIUM structured (any MEDIUM tier)
    → HIGH. Refresh stats only when every claim in this round returned empty (end-of-scope check).
    """
    b = claim(token, limit=120, tier="LOW")
    if b:
        return b, "LOW"

    for ph in ENRICHMENT_PHASES:
        b = claim(token, limit=100, phase=ph)
        if b:
            return b, "MEDIUM_ENRICH"

    # Any remaining MEDIUM (lexicon, biography, councils, etc.) after enrichment phases are drained.
    b = claim(token, limit=50, tier="MEDIUM")
    if b:
        return b, "MEDIUM_STRUCT"

    b = claim(token, limit=10, tier="HIGH")
    if b:
        return b, "HIGH"

    stats = get_stats(token)
    stats_cache.clear()
    stats_cache.update(stats)
    if int(stats.get("totalPending") or 0) == 0:
        return [], None

    if int(stats.get("totalProcessing") or 0) > 0:
        raise RuntimeError(
            f"Queue has pending={stats.get('totalPending')} but claim returned empty "
            f"and processing={stats.get('totalProcessing')} — run unstick or wait."
        )
    return [], None


LLM_BACKEND = "openai"
ANTHROPIC_API_KEY = ""


def _resolve_backend() -> str:
    choice = os.environ.get("LLM_BACKEND", "auto").strip().lower()
    a = os.environ.get("ANTHROPIC_API_KEY", "").strip()
    o = os.environ.get("OPENAI_API_KEY", "").strip()
    if choice == "anthropic":
        return "anthropic" if a else "openai"
    if choice == "openai":
        return "openai"
    # auto
    return "anthropic" if a else "openai"


def main() -> int:
    import urllib.parse # noqa: F401 — used in dev_login via quote

    # Pre-flight: rate-limit ativo?
    ra = _rate_limit_active()
    if ra:
        _jsonl(event="skip", reason="rate_limit_active", until_epoch=ra, remaining_s=ra - int(time.time()))
        return 0

    global LLM_BACKEND, ANTHROPIC_API_KEY
    LLM_BACKEND = _resolve_backend()
    ANTHROPIC_API_KEY = os.environ.get("ANTHROPIC_API_KEY", "").strip()
    api_key = os.environ.get("OPENAI_API_KEY", "").strip()
    if LLM_BACKEND == "anthropic" and not ANTHROPIC_API_KEY:
        print("LLM_BACKEND=anthropic but ANTHROPIC_API_KEY not set", file=sys.stderr)
        return 2
    if LLM_BACKEND == "openai" and not api_key:
        print("OPENAI_API_KEY is not set. Source deploy/.env or export the key.", file=sys.stderr)
        return 2
    _jsonl(event="start", backend=LLM_BACKEND)

    token = dev_login()
    # Default QUEUE_UNSTICK_MINUTES=10 (lease reaper automatic). Set 0 to disable.
    unstuck_env = os.environ.get("QUEUE_UNSTICK_MINUTES", "10").strip() or "10"
    if unstuck_env.isdigit() and int(unstuck_env) > 0:
        r = http_json(
            "POST",
            f"/admin/llm/queue/unstick?staleMinutes={int(unstuck_env)}",
            token=token,
        )
        _jsonl(event="unstick", result=r)

    initial = get_stats(token)
    print(
        json.dumps(
            {
                "initial": {
                    "totalPending": initial.get("totalPending"),
                    "totalProcessing": initial.get("totalProcessing"),
                }
            },
            indent=2,
        )
    )

    if int(initial.get("totalPending") or 0) == 0:
        print("Nothing pending.")
        return 0

    stats_cache: dict[str, Any] = {}

    max_batches = int(os.environ.get("DRAIN_MAX_BATCHES", "0"))

    while True:
        if max_batches and report.batches >= max_batches:
            print(f"Stopping: DRAIN_MAX_BATCHES={max_batches}", flush=True)
            break
        batch, scope = try_claim_next(token, stats_cache)
        if not batch:
            break
        report.batches += 1
        fails_before = len(report.failures)
        done_before = report.items_completed
        _jsonl(event="batch_start", n=report.batches, scope=scope, size=len(batch))
        try:
            run_batch_parallel(token, api_key, batch)
        except OpenAiQuotaExceeded as qe:
            _write_rate_limit_stop()
            _jsonl(event="abort", reason="openai_quota", detail=str(qe)[:400])
            return 3
        except AnthropicQuotaExceeded as qe:
            _write_rate_limit_stop()
            _jsonl(event="abort", reason="anthropic_quota", detail=str(qe)[:400])
            return 3
        _jsonl(
            event="batch_done",
            n=report.batches,
            completed_delta=report.items_completed - done_before,
            failed_delta=len(report.failures) - fails_before,
        )

        for ph in unique_phases(batch):
            msg = apply_phase(token, ph)
            report.phases_applied.add(ph)
            _jsonl(event="apply", phase=ph, result=msg)

    final = get_stats(token)
    summary = {
        "finalPending": final.get("totalPending"),
        "finalProcessing": final.get("totalProcessing"),
        "finalFailed": final.get("totalFailed"),
        "batches": report.batches,
        "itemsCompletedThisRun": report.items_completed,
        "phasesApplied": sorted(report.phases_applied),
        "failures": [{"id": i, "phase": p, "error": e} for i, p, e in report.failures],
    }
    print(json.dumps(summary, indent=2, ensure_ascii=False))
    return 0 if int(final.get("totalPending") or 0) == 0 and not report.failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
