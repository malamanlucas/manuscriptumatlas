"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { useBibleBooks } from "@/hooks/useBible";
import {
  useBibleGlossAuditStats,
  useAuditBibleGlosses,
  useFixFlaggedBibleGlosses,
  useReauditBibleGlosses,
} from "@/hooks/useBibleGlossAudit";
import {
  Loader2,
  Wand2,
  ShieldCheck,
  Hammer,
  RefreshCw,
  CircleAlert,
} from "lucide-react";
import type { GlossAuditExampleDTO } from "@/types";

export function BibleGlossQualityPanel() {
  const t = useTranslations("ingestion.bibleIngestion");
  const booksQuery = useBibleBooks("NT");
  const [book, setBook] = useState<string>("");
  const [chapter, setChapter] = useState<number | null>(null);

  const selectedBookDTO = useMemo(
    () => booksQuery.data?.find((b) => b.name === book) ?? null,
    [booksQuery.data, book]
  );

  const statsQuery = useBibleGlossAuditStats(book || undefined, chapter ?? undefined);
  const auditMutation = useAuditBibleGlosses();
  const fixMutation = useFixFlaggedBibleGlosses();
  const reauditMutation = useReauditBibleGlosses();

  const handleBookChange = (value: string) => {
    setBook(value);
    setChapter(null);
  };

  const runAudit = () => {
    auditMutation.mutate({
      book: book || undefined,
      chapter: chapter ?? undefined,
    });
  };

  const runFix = () => {
    fixMutation.mutate({
      book: book || undefined,
      chapter: chapter ?? undefined,
    });
  };

  const runReaudit = () => {
    if (!window.confirm(t("glossQualityReauditConfirm"))) return;
    reauditMutation.mutate({
      book: book || undefined,
      chapter: chapter ?? undefined,
    });
  };

  const stats = statsQuery.data;
  const hasFlagged = (stats?.badEn ?? 0) + (stats?.badEs ?? 0) + (stats?.badOther ?? 0) > 0;

  return (
    <div className="space-y-4 rounded-lg border border-emerald-500/30 bg-emerald-500/5 p-4">
      <div className="flex items-center gap-2">
        <ShieldCheck className="h-4 w-4 text-emerald-500" />
        <div className="flex-1">
          <h4 className="text-sm font-bold">{t("glossQualityTitle")}</h4>
          <p className="text-xs text-muted-foreground">{t("glossQualitySubtitle")}</p>
        </div>
      </div>

      {/* Escopo */}
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <label className="flex flex-col gap-1 text-xs">
          <span className="font-medium text-muted-foreground">{t("glossQualityScopeBook")}</span>
          <select
            value={book}
            onChange={(e) => handleBookChange(e.target.value)}
            disabled={booksQuery.isLoading}
            className="rounded-md border border-border bg-background px-2 py-1.5 text-sm"
          >
            <option value="">{t("glossQualityScopeAllBooks")}</option>
            {booksQuery.data?.map((b) => (
              <option key={b.id} value={b.name}>
                {b.name}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-1 text-xs">
          <span className="font-medium text-muted-foreground">{t("glossQualityScopeChapter")}</span>
          <select
            value={chapter ?? ""}
            onChange={(e) => setChapter(e.target.value ? Number(e.target.value) : null)}
            disabled={!selectedBookDTO}
            className="rounded-md border border-border bg-background px-2 py-1.5 text-sm disabled:opacity-50"
          >
            <option value="">{t("glossQualityScopeAllChapters")}</option>
            {selectedBookDTO &&
              Array.from({ length: selectedBookDTO.totalChapters }, (_, i) => i + 1).map((ch) => (
                <option key={ch} value={ch}>
                  {ch}
                </option>
              ))}
          </select>
        </label>
      </div>

      {/* Botões */}
      <div className="flex flex-wrap items-center gap-2">
        <button
          onClick={runAudit}
          disabled={auditMutation.isPending}
          className="inline-flex items-center gap-1.5 rounded-lg border border-emerald-500/60 bg-emerald-500/10 px-3 py-1.5 text-xs font-semibold text-emerald-400 hover:bg-emerald-500/20 disabled:opacity-50"
        >
          {auditMutation.isPending ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Wand2 className="h-3.5 w-3.5" />
          )}
          {auditMutation.isPending ? t("glossQualityAuditRunning") : t("glossQualityAudit")}
        </button>

        <button
          onClick={runFix}
          disabled={fixMutation.isPending || !hasFlagged}
          className="inline-flex items-center gap-1.5 rounded-lg border border-amber-500/60 bg-amber-500/10 px-3 py-1.5 text-xs font-semibold text-amber-400 hover:bg-amber-500/20 disabled:opacity-50"
        >
          {fixMutation.isPending ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Hammer className="h-3.5 w-3.5" />
          )}
          {fixMutation.isPending ? t("glossQualityFixRunning") : t("glossQualityFix")}
        </button>

        <button
          onClick={runReaudit}
          disabled={reauditMutation.isPending}
          className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-accent disabled:opacity-50"
        >
          {reauditMutation.isPending ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <RefreshCw className="h-3.5 w-3.5" />
          )}
          {reauditMutation.isPending ? t("glossQualityReauditRunning") : t("glossQualityReaudit")}
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-2 md:grid-cols-6">
        <StatCard label={t("glossQualityStatsTotal")} value={stats?.total ?? 0} tone="neutral" />
        <StatCard label={t("glossQualityStatsOk")} value={stats?.ok ?? 0} tone="good" />
        <StatCard label={t("glossQualityStatsBadEn")} value={stats?.badEn ?? 0} tone="warn" />
        <StatCard label={t("glossQualityStatsBadEs")} value={stats?.badEs ?? 0} tone="warn" />
        <StatCard label={t("glossQualityStatsBadOther")} value={stats?.badOther ?? 0} tone="bad" />
        <StatCard label={t("glossQualityStatsPending")} value={stats?.pending ?? 0} tone="neutral" />
      </div>

      {/* Exemplos */}
      <div className="rounded-md border border-border bg-background">
        <div className="flex items-center gap-2 border-b border-border/60 px-3 py-2">
          <CircleAlert className="h-3.5 w-3.5 text-amber-500" />
          <h5 className="text-xs font-bold">{t("glossQualityExamplesTitle")}</h5>
          {stats && stats.examples.length > 0 && (
            <span className="ml-auto text-[11px] tabular-nums text-muted-foreground">
              {stats.examples.length}
            </span>
          )}
        </div>
        <div className="max-h-72 overflow-y-auto px-3 py-2">
          {statsQuery.isLoading && <p className="text-xs text-muted-foreground">…</p>}
          {stats && stats.examples.length === 0 && (
            <p className="text-xs text-muted-foreground">{t("glossQualityExamplesEmpty")}</p>
          )}
          {stats && stats.examples.length > 0 && (
            <ul className="space-y-1.5">
              {stats.examples.map((ex) => (
                <ExampleRow key={ex.wordId} ex={ex} t={t} />
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Hint */}
      <p className="rounded-md bg-muted/40 px-3 py-2 text-[11px] leading-relaxed text-muted-foreground">
        {t("glossQualityHint", {
          code: "/run-llm bible_audit_glosses_pt",
          apply: "POST /admin/llm/queue/apply/bible_audit_glosses_pt",
        })}
      </p>
    </div>
  );
}

function StatCard({ label, value, tone }: { label: string; value: number; tone: "good" | "warn" | "bad" | "neutral" }) {
  const color =
    tone === "good"
      ? "text-emerald-400 border-emerald-500/30 bg-emerald-500/5"
      : tone === "warn"
        ? "text-amber-400 border-amber-500/30 bg-amber-500/5"
        : tone === "bad"
          ? "text-red-400 border-red-500/30 bg-red-500/5"
          : "text-foreground border-border bg-background";
  return (
    <div className={`rounded-md border px-2.5 py-2 ${color}`}>
      <div className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="text-lg font-bold tabular-nums">{value}</div>
    </div>
  );
}

function ExampleRow({ ex, t }: { ex: GlossAuditExampleDTO; t: ReturnType<typeof useTranslations> }) {
  const badgeColor =
    ex.verdict === "bad_en"
      ? "bg-blue-500/15 text-blue-400 border-blue-500/40"
      : ex.verdict === "bad_es"
        ? "bg-amber-500/15 text-amber-400 border-amber-500/40"
        : "bg-red-500/15 text-red-400 border-red-500/40";
  return (
    <li className="rounded border border-border/40 bg-muted/20 px-2 py-1.5 text-xs">
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-mono text-[11px] text-muted-foreground">
          {ex.book} {ex.chapter}:{ex.verse}
        </span>
        <span className="font-serif text-sm">{ex.originalWord}</span>
        {ex.transliteration && (
          <span className="italic text-[11px] text-muted-foreground">{ex.transliteration}</span>
        )}
        <span className={`ml-auto rounded border px-1.5 py-0.5 text-[10px] font-medium uppercase ${badgeColor}`}>
          {ex.verdict.replace("bad_", "")}
        </span>
      </div>
      <div className="mt-1 flex flex-wrap items-center gap-2 text-[11px]">
        <span className="text-muted-foreground">{t("glossQualityExampleVerdict")}:</span>
        <code className="rounded bg-red-500/10 px-1 text-red-400">{ex.currentGloss}</code>
        {ex.suggestedPt && (
          <>
            <span className="text-muted-foreground">→ {t("glossQualityExampleSuggestion")}:</span>
            <code className="rounded bg-emerald-500/10 px-1 text-emerald-400">{ex.suggestedPt}</code>
          </>
        )}
        {ex.englishGloss && (
          <span className="text-muted-foreground">({ex.englishGloss})</span>
        )}
      </div>
    </li>
  );
}
