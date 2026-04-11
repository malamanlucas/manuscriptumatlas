"use client";

import { useTranslations } from "next-intl";
import { Loader2 } from "lucide-react";
import type { BibleChapterResponse } from "@/types";

interface BibleReaderProps {
  data?: BibleChapterResponse;
  isLoading: boolean;
  error: Error | null;
}

export function BibleReader({ data, isLoading, error }: BibleReaderProps) {
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
        {t("noVerses")}
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-border bg-card p-5 md:p-8">
      <h2 className="mb-6 text-lg font-semibold md:text-xl">
        {data.book} {data.chapter}
        <span className="ml-2 text-sm font-normal text-muted-foreground">({data.version})</span>
      </h2>

      <div className="space-y-1 text-base leading-relaxed md:text-lg md:leading-loose">
        {data.verses.map((v) => (
          <span key={v.verseNumber} className="inline">
            <sup className="mr-1 text-xs font-semibold text-primary/70">{v.verseNumber}</sup>
            <span>{v.text} </span>
          </span>
        ))}
      </div>
    </div>
  );
}
