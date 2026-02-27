"use client";

import Link from "next/link";
import { cn, coverageColor } from "@/lib/utils";
import type { BookCoverage } from "@/types";

interface BookCardProps {
  book: BookCoverage;
  century: number;
}

export function BookCard({ book, century }: BookCardProps) {
  return (
    <Link
      href={`/book/${encodeURIComponent(book.bookName)}?century=${century}`}
      className="group block rounded-xl border border-border bg-card p-4 transition-all hover:shadow-lg hover:border-primary/30"
    >
      <div className="flex items-start justify-between">
        <h3 className="text-sm font-semibold text-card-foreground group-hover:text-primary transition-colors">
          {book.bookName}
        </h3>
        <span
          className={cn(
            "rounded-full px-2 py-0.5 text-xs font-bold",
            coverageColor(book.coveragePercent)
          )}
        >
          {book.coveragePercent.toFixed(1)}%
        </span>
      </div>
      <div className="mt-3">
        <div className="h-2 w-full rounded-full bg-secondary overflow-hidden">
          <div
            className="h-full rounded-full bg-primary transition-all duration-500"
            style={{ width: `${Math.min(book.coveragePercent, 100)}%` }}
          />
        </div>
        <p className="mt-1.5 text-xs text-muted-foreground">
          {book.coveredVerses} / {book.totalVerses} verses
        </p>
      </div>
    </Link>
  );
}
