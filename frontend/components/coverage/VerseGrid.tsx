"use client";

import Link from "next/link";
import { cn } from "@/lib/utils";
import type { ChapterCoverage } from "@/types";

interface VerseGridProps {
  chapters: ChapterCoverage[];
  previousChapters?: ChapterCoverage[];
  bookName?: string;
}

export function VerseGrid({ chapters, previousChapters, bookName }: VerseGridProps) {
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

                const cell = (
                  <span
                    title={
                      bookName
                        ? `Ver manuscritos para ${bookName} ${ch.chapter}:${v}`
                        : `${ch.chapter}:${v} - ${isCovered ? "covered" : "missing"}`
                    }
                    className={cn(
                      "flex h-6 w-6 items-center justify-center rounded text-[10px] font-mono transition-all",
                      isCovered
                        ? isNew
                          ? "bg-amber-400 text-black"
                          : "bg-emerald-500 text-white"
                        : "bg-red-400/80 text-white",
                      bookName && "cursor-pointer hover:ring-2 hover:ring-primary hover:ring-offset-1"
                    )}
                  >
                    {v}
                  </span>
                );

                return bookName ? (
                  <Link
                    key={v}
                    href={`/verse-lookup?book=${encodeURIComponent(bookName)}&chapter=${ch.chapter}&verse=${v}`}
                    className="inline-block"
                  >
                    {cell}
                  </Link>
                ) : (
                  <div key={v}>{cell}</div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
