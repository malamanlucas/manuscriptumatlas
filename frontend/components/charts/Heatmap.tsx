"use client";

import { coverageHeatColor } from "@/lib/utils";
import { useTranslations } from "next-intl";
import type { ChapterCoverage } from "@/types";

interface HeatmapProps {
  chapters: ChapterCoverage[];
  bookName: string;
}

export function Heatmap({ chapters, bookName }: HeatmapProps) {
  const t = useTranslations("heatmap");

  return (
    <div>
      <h3 className="mb-3 text-sm font-semibold">
        {t("chapterHeatmap", { book: bookName })}
      </h3>
      <div className="flex flex-wrap gap-2">
        {chapters.map((ch) => (
          <div
            key={ch.chapter}
            className="flex h-12 w-12 flex-col items-center justify-center rounded-lg text-xs font-bold text-white transition-all hover:scale-110"
            style={{ backgroundColor: coverageHeatColor(ch.coveragePercent) }}
            title={t("chapterTooltip", { chapter: ch.chapter, percent: ch.coveragePercent.toFixed(1) })}
          >
            <span>{ch.chapter}</span>
            <span className="text-[9px] font-normal opacity-80">
              {ch.coveragePercent.toFixed(0)}%
            </span>
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
        <div className="flex items-center gap-1">
          <div
            className="h-3 w-3 rounded"
            style={{ backgroundColor: "#059669" }}
          />
          {t("gte90")}
        </div>
        <div className="flex items-center gap-1">
          <div
            className="h-3 w-3 rounded"
            style={{ backgroundColor: "#34d399" }}
          />
          {t("range60_89")}
        </div>
        <div className="flex items-center gap-1">
          <div
            className="h-3 w-3 rounded"
            style={{ backgroundColor: "#fbbf24" }}
          />
          {t("range30_59")}
        </div>
        <div className="flex items-center gap-1">
          <div
            className="h-3 w-3 rounded"
            style={{ backgroundColor: "#f87171" }}
          />
          {t("lt30")}
        </div>
      </div>
    </div>
  );
}
