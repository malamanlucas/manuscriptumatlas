"use client";

import { useStatsOverview } from "@/hooks/useStats";
import { toRoman } from "@/lib/utils";
import { BookOpen, FileText, Layers, TrendingUp } from "lucide-react";
import { useTranslations } from "next-intl";

export function StatsOverview() {
  const { data, isLoading, error } = useStatsOverview();
  const t = useTranslations("stats");

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="animate-pulse rounded-xl border border-border bg-card p-5"
          >
            <div className="h-8 w-24 rounded bg-secondary" />
          </div>
        ))}
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="rounded-xl border border-red-300 bg-red-50 p-4 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
        {t("failedToLoad")}
      </div>
    );
  }

  const papyrusCount = data.byType?.papyrus ?? 0;
  const uncialCount = data.byType?.uncial ?? 0;

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div className="rounded-xl border border-border bg-card p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-[#4a6fa5]/15 p-2 dark:bg-[#4a6fa5]/20">
            <FileText className="h-5 w-5 text-[#4a6fa5]" />
          </div>
          <div>
            <p className="text-2xl font-bold">
              {data.totalManuscripts.toLocaleString()}
            </p>
            <p className="text-xs text-muted-foreground">
              {t("totalManuscripts")}
            </p>
          </div>
        </div>
      </div>
      <div className="rounded-xl border border-border bg-card p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-[#b8976a]/15 p-2 dark:bg-[#b8976a]/20">
            <Layers className="h-5 w-5 text-[#b8976a]" />
          </div>
          <div>
            <p className="text-2xl font-bold">
              {papyrusCount} / {uncialCount}
            </p>
            <p className="text-xs text-muted-foreground">
              {t("papyriUncials")}
            </p>
          </div>
        </div>
      </div>
      <div className="rounded-xl border border-border bg-card p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-[#5a8a7a]/15 p-2 dark:bg-[#5a8a7a]/20">
            <BookOpen className="h-5 w-5 text-[#5a8a7a]" />
          </div>
          <div>
            <p className="text-2xl font-bold">
              {data.avgBooksPerManuscript.toFixed(1)}
            </p>
            <p className="text-xs text-muted-foreground">
              {t("avgBooks")}
            </p>
          </div>
        </div>
      </div>
      <div className="rounded-xl border border-border bg-card p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-[#7a6e8a]/15 p-2 dark:bg-[#7a6e8a]/20">
            <TrendingUp className="h-5 w-5 text-[#7a6e8a]" />
          </div>
          <div>
            <p className="text-2xl font-bold">
              {data.overallCoveragePercent.toFixed(1)}%
            </p>
            <p className="text-xs text-muted-foreground">
              {t("totalCoverage")}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
