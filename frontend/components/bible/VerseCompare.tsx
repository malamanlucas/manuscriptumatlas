"use client";

import { useTranslations } from "next-intl";
import { Loader2 } from "lucide-react";
import type { BibleCompareResponse } from "@/types";

interface VerseCompareProps {
  data?: BibleCompareResponse;
  isLoading: boolean;
  error: Error | null;
}

export function VerseCompare({ data, isLoading, error }: VerseCompareProps) {
  const t = useTranslations("bible");
  const tc = useTranslations("common");

  if (isLoading) {
    return (
      <div className="rounded-xl border border-border bg-card p-12 text-center">
        <Loader2 className="mx-auto h-6 w-6 animate-spin text-muted-foreground" />
        <p className="mt-2 text-sm text-muted-foreground">{tc("loading")}</p>
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

  if (!data || data.verses.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-12 text-center text-muted-foreground">
        {t("noCompareData")}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {data.verses.map((row) => (
        <div key={row.verseNumber} className="rounded-xl border border-border bg-card p-4 md:p-5">
          <div className="mb-3">
            <span className="rounded-md bg-primary/10 px-2 py-0.5 text-sm font-semibold text-primary">
              {data.book} {data.chapter}:{row.verseNumber}
            </span>
          </div>
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {data.versions.map((ver) => (
              <div key={ver} className="rounded-lg border border-border/50 bg-muted/20 p-3">
                <p className="mb-1 text-xs font-bold text-primary/70">{ver}</p>
                <p className="text-sm leading-relaxed">{row.texts[ver] ?? "—"}</p>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
