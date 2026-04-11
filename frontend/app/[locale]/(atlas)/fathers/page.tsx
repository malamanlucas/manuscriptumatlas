"use client";

import { useState, useMemo } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { FathersTimelineChart } from "@/components/charts/FathersTimelineChart";
import { useChurchFathers, useSearchChurchFathers } from "@/hooks/useChurchFathers";
import { CenturyRangeFilter } from "@/components/filters/CenturyRangeFilter";
import { YearSliderFilter } from "@/components/filters/YearSliderFilter";
import { Link } from "@/i18n/navigation";
import { toRoman } from "@/lib/utils";
import { Search, List, Table2, Calendar, Clock } from "lucide-react";
import { ConfidenceDot } from "@/components/ui/ConfidenceDot";

type TraditionFilter = "all" | "greek" | "latin" | "syriac" | "coptic";

const TRADITION_BADGE_COLORS: Record<string, string> = {
  greek: "bg-[#4a6fa5]/20 text-[#4a6fa5] border-[#4a6fa5]/30",
  latin: "bg-[#b8976a]/20 text-[#b8976a] border-[#b8976a]/30",
  syriac: "bg-[#5a8a7a]/20 text-[#5a8a7a] border-[#5a8a7a]/30",
  coptic: "bg-[#7a6e8a]/20 text-[#7a6e8a] border-[#7a6e8a]/30",
};

const TRADITION_DOT_COLORS: Record<string, string> = {
  greek: "bg-[#4a6fa5]",
  latin: "bg-[#b8976a]",
  syriac: "bg-[#5a8a7a]",
  coptic: "bg-[#7a6e8a]",
};

export default function FathersPage() {
  const t = useTranslations("fathers");
  const tc = useTranslations("common");

  const [selectedCentury, setSelectedCentury] = useState<number | undefined>(undefined);
  const [tradition, setTradition] = useState<TraditionFilter>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [filterMode, setFilterMode] = useState<"century" | "year">("century");
  const [yearSliderPos, setYearSliderPos] = useState<number | undefined>(undefined);

  const [page, setPage] = useState(1);
  const [viewMode, setViewMode] = useState<"compact" | "complete">("compact");

  const tradParam = tradition === "all" ? undefined : tradition;
  const yearMinFrom = filterMode === "year" && yearSliderPos !== undefined ? 0 : undefined;
  const yearMinTo = filterMode === "year" && yearSliderPos !== undefined ? yearSliderPos : undefined;
  const { data, isLoading, error } = useChurchFathers({
    century: filterMode === "century" ? selectedCentury : undefined,
    tradition: tradParam,
    yearMinFrom,
    yearMinTo,
    page,
  });
  const { data: searchResults } = useSearchChurchFathers(searchQuery);

  const displayFathers = searchQuery.length >= 2 ? searchResults ?? [] : data?.fathers ?? [];

  const contemporaries = useMemo(() => {
    if (!selectedCentury || !data?.fathers) return [];
    return data.fathers.filter(
      (f) => f.centuryMin <= selectedCentury && f.centuryMax >= selectedCentury
    );
  }, [selectedCentury, data?.fathers]);

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-6">
        {/* Filters */}
        <div className="rounded-xl border border-border bg-card p-4 md:p-6 space-y-4">
          {/* Filter mode toggle */}
          <div className="flex items-center gap-3">
            <div className="flex items-center rounded-lg border border-border bg-secondary/50 p-0.5">
              <button
                onClick={() => {
                  setFilterMode("century");
                  setYearSliderPos(undefined);
                  setPage(1);
                }}
                className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  filterMode === "century"
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                <Calendar className="h-3.5 w-3.5" />
                {t("filterModeCentury")}
              </button>
              <button
                onClick={() => {
                  setFilterMode("year");
                  setSelectedCentury(undefined);
                  setPage(1);
                }}
                className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  filterMode === "year"
                    ? "bg-primary text-primary-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                <Clock className="h-3.5 w-3.5" />
                {t("filterModeYear")}
              </button>
            </div>
          </div>

          {/* Conditional filter */}
          {filterMode === "century" ? (
            <div>
              <CenturyRangeFilter
                value={selectedCentury}
                onChange={(c) => {
                  setSelectedCentury(c);
                  setPage(1);
                }}
              />
            </div>
          ) : (
            <div>
              <YearSliderFilter
                yearTo={yearSliderPos}
                onYearChange={(to) => {
                  setYearSliderPos(to);
                  setPage(1);
                }}
              />
            </div>
          )}

          <div className="flex flex-col gap-4 md:flex-row md:items-end">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("filterByTradition")}
              </label>
              <div className="flex flex-wrap gap-2">
                {(["all", "greek", "latin", "syriac", "coptic"] as TraditionFilter[]).map(
                  (tr) => (
                    <button
                      key={tr}
                      onClick={() => {
                        setTradition(tr);
                        setPage(1);
                      }}
                      className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                        tradition === tr
                          ? "bg-primary text-primary-foreground"
                          : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                      }`}
                    >
                      {t(`traditions.${tr}`)}
                    </button>
                  )
                )}
              </div>
            </div>

            <div className="min-w-0 md:ml-auto md:w-64">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {tc("search")}
              </label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder={t("searchPlaceholder")}
                  className="w-full rounded-lg border border-border bg-background py-2 pl-9 pr-3 text-sm"
                />
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

        {/* Timeline Chart */}
        {displayFathers.length > 0 && searchQuery.length < 2 && (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            <h2 className="mb-4 text-base font-semibold">{t("timelineTitle")}</h2>
            <FathersTimelineChart
              fathers={displayFathers}
              onBarClick={(century) => setSelectedCentury(century)}
            />
          </div>
        )}

        {/* Contemporaries */}
        {selectedCentury && contemporaries.length > 0 && (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            <h2 className="mb-1 text-base font-semibold">
              {t("contemporaries", { century: toRoman(selectedCentury) })}
            </h2>
            <p className="mb-4 text-sm text-muted-foreground">
              {t("contemporariesDescription", { century: toRoman(selectedCentury) })}
            </p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {contemporaries.map((f) => (
                <Link
                  key={f.id}
                  href={`/fathers/${f.id}`}
                  className="group rounded-lg border border-border bg-background p-4 transition-all hover:border-primary/30 hover:shadow-md"
                >
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-medium group-hover:text-primary">
                      {f.displayName}
                    </h3>
                    <span
                      className={`inline-flex shrink-0 items-center rounded-full border px-2 py-0.5 text-xs font-medium ${
                        TRADITION_BADGE_COLORS[f.tradition] ?? ""
                      }`}
                    >
                      <span className="hidden sm:inline">
                        {t(`traditions.${f.tradition}`)}
                      </span>
                      <span
                        className={`h-2 w-2 rounded-full sm:hidden ${
                          TRADITION_DOT_COLORS[f.tradition] ?? "bg-gray-500"
                        }`}
                      />
                    </span>
                  </div>
                  <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
                    <span>
                      {toRoman(f.centuryMin)}
                      {f.centuryMax !== f.centuryMin && `–${toRoman(f.centuryMax)}`}
                      {f.yearBest ? ` · c. ${f.yearBest} AD` : f.yearMin ? ` · ${f.yearMin}–${f.yearMax} AD` : null}
                    </span>
                    <ConfidenceDot confidence={f.datingConfidence} source={f.datingSource} />
                    {f.primaryLocation && <span> · {f.primaryLocation}</span>}
                  </p>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Table */}
        {displayFathers.length > 0 && (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-base font-semibold">{t("tableTitle")}</h2>
              <div className="flex items-center rounded-lg border border-border bg-secondary/50 p-0.5">
                <button
                  onClick={() => setViewMode("compact")}
                  className={`flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                    viewMode === "compact"
                      ? "bg-primary text-primary-foreground shadow-sm"
                      : "text-muted-foreground hover:text-foreground"
                  }`}
                >
                  <List className="h-3.5 w-3.5" />
                  {t("compactView")}
                </button>
                <button
                  onClick={() => setViewMode("complete")}
                  className={`flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                    viewMode === "complete"
                      ? "bg-primary text-primary-foreground shadow-sm"
                      : "text-muted-foreground hover:text-foreground"
                  }`}
                >
                  <Table2 className="h-3.5 w-3.5" />
                  {t("completeView")}
                </button>
              </div>
            </div>

            {viewMode === "compact" ? (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="py-2 text-left font-medium text-muted-foreground">{t("name")}</th>
                      <th className="py-2 text-left font-medium text-muted-foreground">{t("centuries")}</th>
                      <th className="py-2 text-left font-medium text-muted-foreground">{t("tradition")}</th>
                      <th className="hidden py-2 text-left font-medium text-muted-foreground md:table-cell">{t("location")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {displayFathers.map((f) => (
                      <tr key={f.id} className="border-b border-border/50 hover:bg-secondary/30">
                        <td className="py-2">
                          <Link href={`/fathers/${f.id}`} className="font-medium hover:text-primary hover:underline">
                            {f.displayName}
                          </Link>
                        </td>
                        <td className="py-2">
                          <span className="inline-flex items-center gap-1">
                            <span>
                              {toRoman(f.centuryMin)}
                              {f.centuryMax !== f.centuryMin && `–${toRoman(f.centuryMax)}`}
                              {f.yearBest ? ` · c. ${f.yearBest} AD` : f.yearMin ? ` · ${f.yearMin}–${f.yearMax} AD` : null}
                            </span>
                            <ConfidenceDot confidence={f.datingConfidence} source={f.datingSource} />
                          </span>
                        </td>
                        <td className="py-2">
                          <span className="inline-flex items-center gap-1.5">
                            <span className={`h-2.5 w-2.5 rounded-full ${TRADITION_DOT_COLORS[f.tradition] ?? "bg-gray-500"}`} />
                            <span className="hidden md:inline">{t(`traditions.${f.tradition}`)}</span>
                          </span>
                        </td>
                        <td className="hidden py-2 text-muted-foreground md:table-cell">{f.primaryLocation ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="space-y-3">
                {displayFathers.map((f) => (
                  <div key={f.id} className="rounded-lg border border-border bg-background p-4 hover:border-primary/20 transition-colors">
                    <div className="flex items-start justify-between gap-3">
                      <Link href={`/fathers/${f.id}`} className="font-medium hover:text-primary hover:underline">
                        {f.displayName}
                      </Link>
                      <span className={`inline-flex shrink-0 items-center rounded-full border px-2 py-0.5 text-xs font-medium ${TRADITION_BADGE_COLORS[f.tradition] ?? ""}`}>
                        {t(`traditions.${f.tradition}`)}
                      </span>
                    </div>

                    <div className="mt-3 grid grid-cols-2 gap-x-6 gap-y-2 text-sm sm:grid-cols-3 lg:grid-cols-4">
                      <div>
                        <p className="text-[11px] font-medium text-muted-foreground">{t("centuries")}</p>
                        <p>{toRoman(f.centuryMin)}{f.centuryMax !== f.centuryMin && `–${toRoman(f.centuryMax)}`}</p>
                      </div>
                      <div>
                        <p className="text-[11px] font-medium text-muted-foreground">{t("datingColumn")}</p>
                        <p className="tabular-nums">
                          {f.yearBest ? `c. ${f.yearBest} AD` : f.yearMin ? `${f.yearMin}–${f.yearMax} AD` : "—"}
                        </p>
                      </div>
                      <div>
                        <p className="text-[11px] font-medium text-muted-foreground">
                          <Link href="/methodology#confidence" className="hover:text-primary hover:underline">
                            {t("confidenceColumn")}
                          </Link>
                        </p>
                        <span className="inline-flex items-center gap-1.5">
                          <ConfidenceDot confidence={f.datingConfidence} source={f.datingSource} />
                          <span>{f.datingConfidence ?? "—"}</span>
                        </span>
                      </div>
                      <div>
                        <p className="text-[11px] font-medium text-muted-foreground">{t("sourceColumn")}</p>
                        <p>{f.datingSource ?? "—"}</p>
                      </div>
                      {f.primaryLocation && (
                        <div>
                          <p className="text-[11px] font-medium text-muted-foreground">{t("location")}</p>
                          <p className="text-muted-foreground">{f.primaryLocation}</p>
                        </div>
                      )}
                    </div>

                    {f.datingReference && (
                      <p className="mt-2 text-xs text-muted-foreground/70 line-clamp-2" title={f.datingReference}>
                        {f.datingReference}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}

            {data && data.total > 50 && searchQuery.length < 2 && (
              <div className="mt-4 flex items-center justify-between">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page === 1}
                  className="rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium text-secondary-foreground disabled:opacity-50"
                >
                  {tc("previous")}
                </button>
                <span className="text-sm text-muted-foreground">
                  {page} / {Math.ceil(data.total / 50)}
                </span>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page * 50 >= data.total}
                  className="rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium text-secondary-foreground disabled:opacity-50"
                >
                  {tc("next")}
                </button>
              </div>
            )}
          </div>
        )}

        {!isLoading && displayFathers.length === 0 && (
          <div className="rounded-xl border border-border bg-card p-6 text-center text-muted-foreground">
            {t("noResults")}
          </div>
        )}
      </div>
    </div>
  );
}
