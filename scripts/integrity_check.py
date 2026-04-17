#!/usr/bin/env python3
"""
integrity_check.py — contratos de integridade da llm_prompt_queue + tabelas destino bible_db.

Roda queries standalone (pg via psycopg2 OR psql CLI). Reporta JSON com cada check.
Exit 0 = tudo OK; Exit 1 = violacoes encontradas.

Usage:
  ./scripts/integrity_check.py
  ./scripts/integrity_check.py --json   # saida em JSON puro
  ./scripts/integrity_check.py --fix    # sugere comandos de correcao (nao aplica)

Requires: psql CLI in PATH, DATABASE_URL or (PGHOST, PGUSER, PGPASSWORD, PGPORT) env.
Defaults: host=localhost user=postgres password=postgres port=5432.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from dataclasses import dataclass, field
from typing import Any


def psql(db: str, sql: str) -> list[dict[str, Any]]:
    env = os.environ.copy()
    env.setdefault("PGHOST", "localhost")
    env.setdefault("PGUSER", "postgres")
    env.setdefault("PGPASSWORD", "postgres")
    env.setdefault("PGPORT", "5432")
    cmd = [
        "psql", "-h", env["PGHOST"], "-U", env["PGUSER"], "-p", env["PGPORT"],
        "-d", db, "-A", "-t", "-F", "\t", "-c", sql,
    ]
    r = subprocess.run(cmd, capture_output=True, text=True, env=env, timeout=30)
    if r.returncode != 0:
        raise RuntimeError(f"psql {db} failed: {r.stderr.strip()[:400]}")
    rows = []
    for line in r.stdout.strip().splitlines():
        parts = line.split("\t")
        rows.append({"_cols": parts})
    return rows


@dataclass
class Check:
    name: str
    passed: bool
    detail: str
    violations: int = 0
    fix: str = ""


RESULTS: list[Check] = []


def check(name: str, db: str, sql: str, expected_empty: bool = True, fix: str = "") -> Check:
    """Generic: run SQL, if rows returned (non-empty) → violation."""
    try:
        rows = psql(db, sql)
    except Exception as e:
        c = Check(name=name, passed=False, detail=f"query failed: {e}", violations=-1, fix=fix)
        RESULTS.append(c)
        return c
    # First column typically int count; if > 0 → violation
    count = 0
    if rows and rows[0]["_cols"]:
        try:
            count = int(rows[0]["_cols"][0])
        except ValueError:
            count = len(rows)
    passed = (count == 0) if expected_empty else (count > 0)
    detail = f"count={count}"
    c = Check(name=name, passed=passed, detail=detail, violations=count, fix=fix)
    RESULTS.append(c)
    return c


def main() -> int:
    # ── 1. Orphaned claims: processing > 10 min sem update
    check(
        "no_orphaned_claims",
        "nt_coverage",
        """SELECT COUNT(*) FROM llm_prompt_queue
           WHERE status='processing'
             AND claimed_at IS NOT NULL
             AND claimed_at < NOW() - INTERVAL '10 minutes';""",
        fix="curl -X POST $BACKEND/admin/llm/queue/unstick?staleMinutes=10",
    )

    # ── 2. response_content nulo em items nao-pending
    check(
        "response_content_present",
        "nt_coverage",
        """SELECT COUNT(*) FROM llm_prompt_queue
           WHERE status IN ('completed','applied','failed')
             AND (response_content IS NULL OR LENGTH(response_content)=0);""",
        fix="Investigar item-por-item: SELECT id,label FROM llm_prompt_queue WHERE ...",
    )

    # ── 3. callback_context parse-able (JSON valido)
    check(
        "callback_context_parseable",
        "nt_coverage",
        """SELECT COUNT(*) FROM llm_prompt_queue
           WHERE status <> 'pending'
             AND callback_context IS NOT NULL
             AND NOT (callback_context::jsonb IS NOT NULL);""",
        fix="Drop/reset items com callback_context malformado",
    )

    # ── 4. Glosses applied nao pode deixar palavra ainda NULL com transliteration do chunk
    # (phantom-applied: applied sem efeito no destino). Simplificado: ratio applied vs preenchidos.
    check(
        "glosses_applied_reflects_destination",
        "bible_translate_glosses",  # comparamos 2 bancos — pulando via query manual
        """SELECT 0;""",  # placeholder — integrity real e cross-db; ver detalhe abaixo
    )
    # Cross-DB check: contar applied items vs palavras com gloss nao-nulo
    try:
        q = psql("nt_coverage",
                 "SELECT COUNT(*) FROM llm_prompt_queue "
                 "WHERE phase_name='bible_translate_glosses' AND status='applied';")
        applied_n = int(q[0]["_cols"][0]) if q else 0
        b = psql("bible_db",
                 "SELECT "
                 "COUNT(*) FILTER (WHERE portuguese_gloss IS NOT NULL), "
                 "COUNT(*) FILTER (WHERE spanish_gloss IS NOT NULL) "
                 "FROM interlinear_words WHERE language='greek';")
        pt, es = 0, 0
        if b and len(b[0]["_cols"]) >= 2:
            pt = int(b[0]["_cols"][0])
            es = int(b[0]["_cols"][1])
        # Heuristica: applied items deveriam corresponder a pelo menos N palavras (N = applied * ~10 items/chunk)
        avg_words_per_applied = (pt + es) / max(1, applied_n * 2)
        passed = avg_words_per_applied > 3.0  # soft check — cada applied item/locale cobre >3 palavras
        RESULTS.append(Check(
            name="glosses_applied_reflects_destination",
            passed=passed,
            detail=f"applied={applied_n} pt={pt} es={es} avg_words_per_half_applied={avg_words_per_applied:.1f}",
            violations=0 if passed else 1,
            fix="Investigar phantom-applied: items com status=applied mas que nao populou interlinear_words",
        ))
        RESULTS.pop(-2)  # remove placeholder
    except Exception as e:
        RESULTS.append(Check(
            name="glosses_applied_reflects_destination",
            passed=False,
            detail=f"cross-db query failed: {e}",
            violations=-1,
        ))
        RESULTS.pop(-2)

    # ── 5. Lexicon hebrew: row em translations com short_definition NULL onde base tem
    check(
        "hebrew_pt_translations_consistent",
        "bible_db",
        """SELECT COUNT(*) FROM hebrew_lexicon hl
           LEFT JOIN hebrew_lexicon_translations t
             ON t.lexicon_id=hl.id AND t.locale='pt'
           WHERE hl.short_definition IS NOT NULL
             AND (t.short_definition IS NULL OR LENGTH(t.short_definition)=0);""",
        fix="POST /admin/bible/ingestion/run/bible_translate_hebrew_lexicon + apply",
    )

    # ── 6. Duplicatas: mesmo callback_context+phase em status=applied duplicado
    check(
        "no_duplicate_applied",
        "nt_coverage",
        """SELECT COUNT(*) FROM (
             SELECT phase_name, md5(callback_context) AS h
             FROM llm_prompt_queue
             WHERE status='applied' AND callback_context IS NOT NULL
             GROUP BY phase_name, md5(callback_context)
             HAVING COUNT(*) > 1
           ) dup;""",
        fix="SELECT phase_name, md5(callback_context), COUNT(*) FROM ... GROUP BY ... HAVING COUNT>1",
    )

    # ── Summary
    total = len(RESULTS)
    passed = sum(1 for c in RESULTS if c.passed)
    failed = total - passed

    as_json = "--json" in sys.argv
    if as_json:
        print(json.dumps({
            "summary": {"total": total, "passed": passed, "failed": failed},
            "checks": [c.__dict__ for c in RESULTS],
        }, indent=2, ensure_ascii=False))
    else:
        print(f"Integrity: {passed}/{total} passed")
        for c in RESULTS:
            mark = "OK " if c.passed else "XX "
            print(f"  {mark}{c.name} — {c.detail}")
            if not c.passed and c.fix:
                print(f"     fix: {c.fix}")

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
