"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { TimelineChart } from "@/components/charts/TimelineChart";
import { useTimeline } from "@/hooks/useTimeline";
import { NT_BOOKS, toRoman } from "@/lib/utils";

type FilterType = "all" | "papyrus" | "uncial";

export default function TimelinePage() {
  const t = useTranslations("timeline");
  const tc = useTranslations("common");
  const tBooks = useTranslations("books");

  const [book, setBook] = useState<string | undefined>(undefined);
  const [filterType, setFilterType] = useState<FilterType>("all");
  const [showDelta, setShowDelta] = useState(false);

  const type = filterType === "all" ? undefined : filterType;
  const { data, isLoading, error } = useTimeline(book, type);

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("bookFilter")}
              </label>
              <select
                value={book ?? ""}
                onChange={(e) =>
                  setBook(e.target.value === "" ? undefined : e.target.value)
                }
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                <option value="">{t("fullNT")}</option>
                <optgroup label={t("gospels")}>
                  {NT_BOOKS.slice(0, 4).map((b) => (
                    <option key={b} value={b}>
                      {tBooks(b)}
                    </option>
                  ))}
                </optgroup>
                <optgroup label={t("otherBooks")}>
                  {NT_BOOKS.slice(4).map((b) => (
                    <option key={b} value={b}>
                      {tBooks(b)}
                    </option>
                  ))}
                </optgroup>
              </select>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("manuscriptType")}
              </label>
              <div className="flex gap-2">
                {(["all", "papyrus", "uncial"] as FilterType[]).map((ft) => (
                  <button
                    key={ft}
                    onClick={() => setFilterType(ft)}
                    className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      filterType === ft
                        ? "bg-primary text-primary-foreground"
                        : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                    }`}
                  >
                    {ft === "all" ? tc("all") : ft === "papyrus" ? tc("papyri") : tc("uncials")}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("viewLabel")}
              </label>
              <div className="flex gap-2">
                <button
                  onClick={() => setShowDelta(false)}
                  className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    !showDelta
                      ? "bg-primary text-primary-foreground"
                      : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
                >
                  {t("cumulative")}
                </button>
                <button
                  onClick={() => setShowDelta(true)}
                  className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    showDelta
                      ? "bg-primary text-primary-foreground"
                      : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
                >
                  {t("delta")}
                </button>
              </div>
            </div>
          </div>
        </div>

        {isLoading && (
          <div className="animate-pulse rounded-xl border border-border bg-card p-6">
            <div className="h-[350px] rounded bg-secondary" />
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {(error as Error).message}
          </div>
        )}

        {data && (
          <>
            <div className="rounded-xl border border-border bg-card p-6">
              <h2 className="mb-4 text-base font-semibold">
                {book ? tBooks(book) : "New Testament"} &mdash;{" "}
                {showDelta ? t("newVersesPerCentury") : t("cumulativeCoverage")}
                {type && ` (${type})`}
              </h2>
              <TimelineChart entries={data.entries} showDelta={showDelta} />
            </div>

            <div className="rounded-xl border border-border bg-card p-6">
              <h2 className="mb-4 text-base font-semibold">
                {t("centuryByCenturyData")}
              </h2>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="py-2 text-left font-medium text-muted-foreground">
                        {tc("century")}
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        {t("cumulativePercent")}
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        {t("covered")}
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        {t("newVerses")}
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        {t("growthPercent")}
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        {t("missing")}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.entries.map((e) => (
                      <tr
                        key={e.century}
                        className="border-b border-border/50 hover:bg-secondary/30"
                      >
                        <td className="py-2 font-medium">
                          {toRoman(e.century)}
                        </td>
                        <td className="py-2 text-right">
                          {e.cumulativePercent.toFixed(2)}%
                        </td>
                        <td className="py-2 text-right">
                          {e.coveredVerses.toLocaleString()}
                        </td>
                        <td className="py-2 text-right text-emerald-600 dark:text-emerald-400">
                          +{e.newVersesCount.toLocaleString()}
                        </td>
                        <td className="py-2 text-right">
                          {e.growthPercent.toFixed(2)}%
                        </td>
                        <td className="py-2 text-right text-muted-foreground">
                          {e.missingVersesCount.toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
