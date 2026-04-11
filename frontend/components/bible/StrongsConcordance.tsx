"use client";

import { useTranslations } from "next-intl";
import { Loader2, ChevronLeft, ChevronRight } from "lucide-react";
import Link from "next/link";
import type { StrongsConcordanceDTO } from "@/types";

interface StrongsConcordanceProps {
  data?: StrongsConcordanceDTO;
  isLoading: boolean;
  error: Error | null;
  onPageChange: (page: number) => void;
}

export function StrongsConcordance({ data, isLoading, error, onPageChange }: StrongsConcordanceProps) {
  const t = useTranslations("bible");
  const tc = useTranslations("common");

  if (isLoading) {
    return (
      <div className="rounded-xl border border-border bg-card p-12 text-center">
        <Loader2 className="mx-auto h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
        {tc("failedToLoad", { error: error.message })}
      </div>
    );
  }

  if (!data || data.occurrences.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-12 text-center text-muted-foreground">
        {t("noOccurrences")}
      </div>
    );
  }

  const totalPages = Math.ceil(data.totalOccurrences / data.limit);

  return (
    <div className="rounded-xl border border-border bg-card p-5 md:p-6">
      <div className="mb-4">
        <h3 className="text-base font-bold">
          {t("occurrences")} <span className="text-muted-foreground font-normal">({data.totalOccurrences})</span>
        </h3>
      </div>

      <div className="overflow-x-auto -mx-5 px-5 md:-mx-6 md:px-6">
        <table className="w-full text-sm min-w-[500px]">
          <thead>
            <tr className="border-b-2 border-border text-left">
              <th className="pb-3 pr-4 text-xs font-bold uppercase tracking-wider text-muted-foreground">{t("reference")}</th>
              <th className="pb-3 pr-4 text-xs font-bold uppercase tracking-wider text-muted-foreground">{t("originalWord")}</th>
              <th className="pb-3 pr-4 text-xs font-bold uppercase tracking-wider text-muted-foreground">{t("lemmaLabel")}</th>
              <th className="pb-3 text-xs font-bold uppercase tracking-wider text-muted-foreground">{t("morphologyLabel")}</th>
            </tr>
          </thead>
          <tbody>
            {data.occurrences.map((occ, idx) => (
              <tr key={idx} className="border-b border-border/30 hover:bg-muted/20 transition-colors last:border-0">
                <td className="py-2.5 pr-4">
                  <Link
                    href={`/bible/KJV/${encodeURIComponent(occ.book)}/${occ.chapter}`}
                    className="text-sm font-medium text-primary hover:underline whitespace-nowrap"
                  >
                    {occ.book} {occ.chapter}:{occ.verseNumber}
                  </Link>
                </td>
                <td className="py-2.5 pr-4 text-base font-semibold">{occ.originalWord}</td>
                <td className="py-2.5 pr-4 text-muted-foreground">{occ.lemma}</td>
                <td className="py-2.5 font-mono text-xs text-muted-foreground">{occ.morphology ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-3">
          <button
            onClick={() => onPageChange(Math.max(1, data.page - 1))}
            disabled={data.page <= 1}
            className="rounded-lg border border-border p-2 hover:bg-muted disabled:opacity-30"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-sm text-muted-foreground">
            {data.page} / {totalPages}
          </span>
          <button
            onClick={() => onPageChange(Math.min(totalPages, data.page + 1))}
            disabled={data.page >= totalPages}
            className="rounded-lg border border-border p-2 hover:bg-muted disabled:opacity-30"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );
}
