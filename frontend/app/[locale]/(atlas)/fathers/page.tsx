"use client";

import { useState, useMemo } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { FathersTimelineChart } from "@/components/charts/FathersTimelineChart";
import { useChurchFathers, useSearchChurchFathers } from "@/hooks/useChurchFathers";
import { CenturyRangeFilter } from "@/components/filters/CenturyRangeFilter";
import { Link } from "@/i18n/navigation";
import { toRoman } from "@/lib/utils";
import { Search } from "lucide-react";

type TraditionFilter = "all" | "greek" | "latin" | "syriac" | "coptic";

const TRADITION_BADGE_COLORS: Record<string, string> = {
  greek: "bg-blue-500/20 text-blue-400 border-blue-500/30",
  latin: "bg-amber-500/20 text-amber-400 border-amber-500/30",
  syriac: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  coptic: "bg-purple-500/20 text-purple-400 border-purple-500/30",
};

const TRADITION_DOT_COLORS: Record<string, string> = {
  greek: "bg-blue-500",
  latin: "bg-amber-500",
  syriac: "bg-emerald-500",
  coptic: "bg-purple-500",
};

export default function FathersPage() {
  const t = useTranslations("fathers");
  const tc = useTranslations("common");

  const [selectedCentury, setSelectedCentury] = useState<number | undefined>(undefined);
  const [tradition, setTradition] = useState<TraditionFilter>("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(1);

  const tradParam = tradition === "all" ? undefined : tradition;
  const { data, isLoading, error } = useChurchFathers({
    century: selectedCentury,
    tradition: tradParam,
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

      <div className="space-y-6 p-4 md:p-6">
        {/* Filters */}
        <div className="rounded-xl border border-border bg-card p-4 md:p-6 space-y-4">
          <div>
            <label className="mb-2 block text-xs font-medium text-muted-foreground">
              {t("filterByCentury")}
            </label>
            <CenturyRangeFilter
              value={selectedCentury}
              onChange={(c) => {
                setSelectedCentury(c);
                setPage(1);
              }}
            />
          </div>

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
                  <p className="mt-1 text-sm text-muted-foreground">
                    {toRoman(f.centuryMin)}
                    {f.centuryMax !== f.centuryMin && `–${toRoman(f.centuryMax)}`}
                    {f.primaryLocation && ` · ${f.primaryLocation}`}
                  </p>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Table */}
        {displayFathers.length > 0 && (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            <h2 className="mb-4 text-base font-semibold">{t("tableTitle")}</h2>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    <th className="py-2 text-left font-medium text-muted-foreground">
                      {t("name")}
                    </th>
                    <th className="py-2 text-left font-medium text-muted-foreground">
                      {t("centuries")}
                    </th>
                    <th className="py-2 text-left font-medium text-muted-foreground">
                      {t("tradition")}
                    </th>
                    <th className="hidden py-2 text-left font-medium text-muted-foreground md:table-cell">
                      {t("location")}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {displayFathers.map((f) => (
                    <tr
                      key={f.id}
                      className="border-b border-border/50 hover:bg-secondary/30"
                    >
                      <td className="py-2">
                        <Link
                          href={`/fathers/${f.id}`}
                          className="font-medium hover:text-primary hover:underline"
                        >
                          {f.displayName}
                        </Link>
                      </td>
                      <td className="py-2">
                        {toRoman(f.centuryMin)}
                        {f.centuryMax !== f.centuryMin &&
                          `–${toRoman(f.centuryMax)}`}
                      </td>
                      <td className="py-2">
                        <span className="inline-flex items-center gap-1.5">
                          <span
                            className={`h-2.5 w-2.5 rounded-full ${
                              TRADITION_DOT_COLORS[f.tradition] ?? "bg-gray-500"
                            }`}
                          />
                          <span className="hidden md:inline">
                            {t(`traditions.${f.tradition}`)}
                          </span>
                        </span>
                      </td>
                      <td className="hidden py-2 text-muted-foreground md:table-cell">
                        {f.primaryLocation ?? "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

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
