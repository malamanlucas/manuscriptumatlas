"use client";

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { TimelineChart } from "@/components/charts/TimelineChart";
import { useTimeline } from "@/hooks/useTimeline";
import { NT_BOOKS, toRoman } from "@/lib/utils";

type FilterType = "all" | "papyrus" | "uncial";

export default function TimelinePage() {
  const [book, setBook] = useState<string | undefined>(undefined);
  const [filterType, setFilterType] = useState<FilterType>("all");
  const [showDelta, setShowDelta] = useState(false);

  const type = filterType === "all" ? undefined : filterType;
  const { data, isLoading, error } = useTimeline(book, type);

  return (
    <div className="min-h-screen">
      <Header
        title="Timeline"
        subtitle="Evolutionary Coverage Over Centuries"
      />

      <div className="p-6 space-y-6">
        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center">
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                Book Filter
              </label>
              <select
                value={book ?? ""}
                onChange={(e) =>
                  setBook(e.target.value === "" ? undefined : e.target.value)
                }
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                <option value="">Full New Testament</option>
                <optgroup label="Gospels">
                  {NT_BOOKS.slice(0, 4).map((b) => (
                    <option key={b} value={b}>
                      {b}
                    </option>
                  ))}
                </optgroup>
                <optgroup label="Other Books">
                  {NT_BOOKS.slice(4).map((b) => (
                    <option key={b} value={b}>
                      {b}
                    </option>
                  ))}
                </optgroup>
              </select>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                Manuscript Type
              </label>
              <div className="flex gap-2">
                {(["all", "papyrus", "uncial"] as FilterType[]).map((t) => (
                  <button
                    key={t}
                    onClick={() => setFilterType(t)}
                    className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      filterType === t
                        ? "bg-primary text-primary-foreground"
                        : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                    }`}
                  >
                    {t === "all" ? "All" : t === "papyrus" ? "Papyri" : "Uncials"}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                View
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
                  Cumulative
                </button>
                <button
                  onClick={() => setShowDelta(true)}
                  className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    showDelta
                      ? "bg-primary text-primary-foreground"
                      : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
                >
                  Delta
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
                {book ?? "New Testament"} &mdash;{" "}
                {showDelta ? "New Verses per Century" : "Cumulative Coverage"}
                {type && ` (${type})`}
              </h2>
              <TimelineChart entries={data.entries} showDelta={showDelta} />
            </div>

            <div className="rounded-xl border border-border bg-card p-6">
              <h2 className="mb-4 text-base font-semibold">
                Century-by-Century Data
              </h2>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="py-2 text-left font-medium text-muted-foreground">
                        Century
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        Cumulative %
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        Covered
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        New Verses
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        Growth %
                      </th>
                      <th className="py-2 text-right font-medium text-muted-foreground">
                        Missing
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
