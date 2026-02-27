"use client";

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { CenturySlider } from "@/components/coverage/CenturySlider";
import { BookCard } from "@/components/coverage/BookCard";
import { GospelPanel } from "@/components/coverage/GospelPanel";
import { StatsOverview } from "@/components/stats/StatsOverview";
import { useCoverageByCentury } from "@/hooks/useCoverage";
import { toRoman, BOOK_CATEGORIES } from "@/lib/utils";
import { BookOpen, CheckCircle, AlertTriangle, TrendingUp } from "lucide-react";

export default function DashboardPage() {
  const [century, setCentury] = useState(5);
  const [type, setType] = useState<string | undefined>(undefined);
  const { data, isLoading, error } = useCoverageByCentury(century, type);

  return (
    <div className="min-h-screen">
      <Header
        title="Dashboard"
        subtitle="NT Manuscript Coverage Overview"
      />

      <div className="p-6 space-y-6">
        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div className="flex-1">
              <CenturySlider value={century} onChange={setCentury} />
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setType(undefined)}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  !type
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                All
              </button>
              <button
                onClick={() => setType("papyrus")}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "papyrus"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                Papyri
              </button>
              <button
                onClick={() => setType("uncial")}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "uncial"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                Uncials
              </button>
            </div>
          </div>
        </div>

        {isLoading && (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="animate-pulse rounded-xl border border-border bg-card p-6"
              >
                <div className="h-8 w-20 rounded bg-secondary" />
              </div>
            ))}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            Failed to load data: {(error as Error).message}
          </div>
        )}

        <div className="space-y-2">
          <h2 className="text-base font-semibold">Global Statistics</h2>
          <StatsOverview />
        </div>

        {data && (
          <>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-blue-100 p-2 dark:bg-blue-900">
                    <TrendingUp className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {data.summary.overallCoveragePercent.toFixed(1)}%
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Overall Coverage (Century {toRoman(century)})
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-emerald-100 p-2 dark:bg-emerald-900">
                    <CheckCircle className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {data.summary.coveredVerses.toLocaleString()}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Verses Covered
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-purple-100 p-2 dark:bg-purple-900">
                    <BookOpen className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {data.fullyAttested.length}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Fully Attested Books
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-amber-100 p-2 dark:bg-amber-900">
                    <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {(
                        data.summary.totalNtVerses - data.summary.coveredVerses
                      ).toLocaleString()}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Missing Verses
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <GospelPanel century={century} type={type} />

            {Object.entries(BOOK_CATEGORIES).map(([category, books]) => (
              <div key={category}>
                <h2 className="mb-3 text-base font-semibold">{category}</h2>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
                  {books.map((bookName) => {
                    const book = data.books.find(
                      (b) => b.bookName === bookName
                    );
                    if (!book) return null;
                    return (
                      <BookCard
                        key={book.bookName}
                        book={book}
                        century={century}
                      />
                    );
                  })}
                </div>
              </div>
            ))}
          </>
        )}
      </div>
    </div>
  );
}
