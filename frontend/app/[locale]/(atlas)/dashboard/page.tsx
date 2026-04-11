"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { CenturySlider } from "@/components/coverage/CenturySlider";
import { BookCard } from "@/components/coverage/BookCard";
import { GospelPanel } from "@/components/coverage/GospelPanel";
import { StatsOverview } from "@/components/stats/StatsOverview";
import { useCoverageByCentury } from "@/hooks/useCoverage";
import { toRoman } from "@/lib/utils";
import { BookOpen, CheckCircle, AlertTriangle, TrendingUp } from "lucide-react";

const CATEGORY_KEYS: Record<string, string[]> = {
  Gospels: ["Matthew", "Mark", "Luke", "John"],
  History: ["Acts"],
  PaulineEpistles: [
    "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
    "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
    "1 Timothy", "2 Timothy", "Titus", "Philemon",
  ],
  GeneralEpistles: ["Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude"],
  Apocalypse: ["Revelation"],
};

export default function DashboardPage() {
  const t = useTranslations("dashboard");
  const tc = useTranslations("common");
  const tCat = useTranslations("bookCategories");

  const [century, setCentury] = useState(5);
  const [type, setType] = useState<string | undefined>(undefined);
  const { data, isLoading, error } = useCoverageByCentury(century, type);

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
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
                {tc("all")}
              </button>
              <button
                onClick={() => setType("papyrus")}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "papyrus"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                {tc("papyri")}
              </button>
              <button
                onClick={() => setType("uncial")}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "uncial"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                {tc("uncials")}
              </button>
            </div>
          </div>
        </div>

        {isLoading && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-4">
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
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        <div className="space-y-2">
          <h2 className="text-base font-semibold">{t("globalStats")}</h2>
          <StatsOverview />
        </div>

        {data && (
          <>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-4">
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-[#4a6fa5]/15 p-2 dark:bg-[#4a6fa5]/20">
                    <TrendingUp className="h-5 w-5 text-[#4a6fa5]" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {data.summary.overallCoveragePercent.toFixed(1)}%
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("overallCoverage", { century: toRoman(century) })}
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-[#5a8a7a]/15 p-2 dark:bg-[#5a8a7a]/20">
                    <CheckCircle className="h-5 w-5 text-[#5a8a7a]" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {data.summary.coveredVerses.toLocaleString()}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("versesCovered")}
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-[#7a6e8a]/15 p-2 dark:bg-[#7a6e8a]/20">
                    <BookOpen className="h-5 w-5 text-[#7a6e8a]" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {data.fullyAttested.length}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("fullyAttestedBooks")}
                    </p>
                  </div>
                </div>
              </div>
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-[#b8976a]/15 p-2 dark:bg-[#b8976a]/20">
                    <AlertTriangle className="h-5 w-5 text-[#b8976a]" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold">
                      {(
                        data.summary.totalNtVerses - data.summary.coveredVerses
                      ).toLocaleString()}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("missingVerses")}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <GospelPanel century={century} type={type} />

            {Object.entries(CATEGORY_KEYS).map(([categoryKey, books]) => (
              <div key={categoryKey}>
                <h2 className="mb-3 text-base font-semibold">{tCat(categoryKey)}</h2>
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
