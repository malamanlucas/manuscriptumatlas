"use client";

import { useGospelCoverage } from "@/hooks/useCoverage";
import { cn, coverageColor } from "@/lib/utils";

interface GospelPanelProps {
  century: number;
  type?: string;
}

export function GospelPanel({ century, type }: GospelPanelProps) {
  const { data, isLoading } = useGospelCoverage(century, type);

  if (isLoading) {
    return (
      <div className="animate-pulse rounded-xl border border-border bg-card p-6">
        <div className="h-4 w-32 rounded bg-secondary mb-4" />
        <div className="grid grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-20 rounded bg-secondary" />
          ))}
        </div>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="rounded-xl border border-border bg-card p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-base font-semibold">Gospels</h2>
        <span
          className={cn(
            "rounded-full px-3 py-1 text-sm font-bold",
            coverageColor(data.aggregated.overallCoveragePercent)
          )}
        >
          {data.aggregated.overallCoveragePercent.toFixed(1)}%
        </span>
      </div>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        {data.individual.map((book) => (
          <div
            key={book.bookName}
            className="rounded-lg bg-secondary/50 p-3 text-center"
          >
            <p className="text-sm font-medium">{book.bookName}</p>
            <p className="mt-1 text-2xl font-bold">
              {book.coveragePercent.toFixed(1)}%
            </p>
            <p className="text-xs text-muted-foreground">
              {book.coveredVerses}/{book.totalVerses}
            </p>
          </div>
        ))}
      </div>
      {data.missingVerses.length > 0 && (
        <p className="mt-3 text-xs text-muted-foreground">
          {data.missingVerses.length} verses missing across all gospels
        </p>
      )}
    </div>
  );
}
