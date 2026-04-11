"use client";

import { useTranslations } from "next-intl";
import { Loader2, ChevronLeft, ChevronRight } from "lucide-react";
import Link from "next/link";
import type { BibleSearchResponse } from "@/types";

interface BibleSearchResultsProps {
  data?: BibleSearchResponse;
  isLoading: boolean;
  error: Error | null;
  onPageChange: (page: number) => void;
}

export function BibleSearchResults({ data, isLoading, error, onPageChange }: BibleSearchResultsProps) {
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

  if (!data) return null;

  if (data.isReference && data.resolvedReference) {
    const ref = data.resolvedReference;
    return (
      <div className="rounded-xl border border-border bg-card p-5">
        <p className="text-sm text-muted-foreground mb-2">{t("referenceDetected")}</p>
        <Link
          href={`/bible/KJV/${encodeURIComponent(ref.book)}/${ref.chapter}`}
          className="text-lg font-semibold text-primary hover:underline"
        >
          {ref.book} {ref.chapter}{ref.verseStart ? `:${ref.verseStart}` : ""}
          {ref.verseEnd && ref.verseEnd !== ref.verseStart ? `-${ref.verseEnd}` : ""}
        </Link>
      </div>
    );
  }

  if (data.results.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-12 text-center text-muted-foreground">
        {t("noSearchResults")}
      </div>
    );
  }

  const totalPages = Math.ceil(data.totalResults / data.limit);

  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        {t("searchResults", { count: data.totalResults })}
      </p>

      {data.results.map((r, idx) => (
        <div key={idx} className="rounded-xl border border-border bg-card p-4">
          <div className="flex items-center gap-2 mb-2">
            <Link
              href={`/bible/${r.version}/${encodeURIComponent(r.book)}/${r.chapter}`}
              className="text-sm font-semibold text-primary hover:underline"
            >
              {r.book} {r.chapter}:{r.verseNumber}
            </Link>
            <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{r.version}</span>
          </div>
          <p className="text-sm leading-relaxed">{r.snippet ?? r.text}</p>
        </div>
      ))}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-2">
          <button
            onClick={() => onPageChange(Math.max(1, data.page - 1))}
            disabled={data.page <= 1}
            className="rounded-lg border border-border p-2 hover:bg-muted disabled:opacity-30"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-sm text-muted-foreground">{data.page} / {totalPages}</span>
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
