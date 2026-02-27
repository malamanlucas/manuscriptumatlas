"use client";

import { cn } from "@/lib/utils";
import type { ChapterCoverage } from "@/types";

interface VerseGridProps {
  chapters: ChapterCoverage[];
  previousChapters?: ChapterCoverage[];
}

export function VerseGrid({ chapters, previousChapters }: VerseGridProps) {
  const prevCoveredMap = new Map<string, Set<number>>();
  if (previousChapters) {
    for (const ch of previousChapters) {
      prevCoveredMap.set(String(ch.chapter), new Set(ch.coveredList));
    }
  }

  return (
    <div className="space-y-4">
      {chapters.map((ch) => {
        const prevCovered = prevCoveredMap.get(String(ch.chapter));
        const totalVerses = ch.coveredList.length + ch.missingList.length;
        const allVerses = Array.from({ length: totalVerses }, (_, i) => i + 1);

        return (
          <div key={ch.chapter} className="rounded-lg border border-border p-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-sm font-semibold">
                Chapter {ch.chapter}
              </span>
              <span className="text-xs text-muted-foreground">
                {ch.coveredVerses}/{ch.totalVerses} ({ch.coveragePercent.toFixed(1)}%)
              </span>
            </div>
            <div className="flex flex-wrap gap-1">
              {allVerses.map((v) => {
                const isCovered = ch.coveredList.includes(v);
                const isNew =
                  isCovered && prevCovered && !prevCovered.has(v);

                return (
                  <div
                    key={v}
                    title={`${ch.chapter}:${v} - ${isCovered ? "covered" : "missing"}`}
                    className={cn(
                      "flex h-6 w-6 items-center justify-center rounded text-[10px] font-mono transition-all",
                      isCovered
                        ? isNew
                          ? "bg-amber-400 text-black"
                          : "bg-emerald-500 text-white"
                        : "bg-red-400/80 text-white"
                    )}
                  >
                    {v}
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
