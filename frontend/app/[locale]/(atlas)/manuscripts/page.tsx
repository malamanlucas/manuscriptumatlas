"use client";

import { useState } from "react";
import { Link } from "@/i18n/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useManuscripts } from "@/hooks/useManuscripts";
import { toRoman } from "@/lib/utils";
import { ExternalLink } from "lucide-react";
import { YearRangeFilter } from "@/components/filters/YearRangeFilter";

export default function ManuscriptsPage() {
  const t = useTranslations("manuscripts");
  const tc = useTranslations("common");

  const [type, setType] = useState<string | undefined>("papyrus");
  const [century, setCentury] = useState<number | undefined>(undefined);
  const [filterYearMin, setFilterYearMin] = useState<number | undefined>(undefined);
  const [filterYearMax, setFilterYearMax] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(1);

  const { data, isLoading, error } = useManuscripts({
    type,
    century: (filterYearMin || filterYearMax) ? undefined : century,
    yearMin: filterYearMin,
    yearMax: filterYearMax,
    page,
    limit: 50,
  });

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="p-4 md:p-6 space-y-6">
        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
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
                onClick={() => { setType("papyrus"); setPage(1); }}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "papyrus"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                {tc("papyri")}
              </button>
              <button
                onClick={() => { setType("uncial"); setPage(1); }}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "uncial"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                {tc("uncials")}
              </button>
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm text-muted-foreground whitespace-nowrap">{t("centuryFilter")}</label>
              <select
                value={century ?? ""}
                onChange={(e) => {
                  const v = e.target.value;
                  setCentury(v ? parseInt(v, 10) : undefined);
                  setFilterYearMin(undefined);
                  setFilterYearMax(undefined);
                  setPage(1);
                }}
                className="rounded-lg border border-input bg-background px-3 py-1.5 text-sm"
              >
                <option value="">{tc("all")}</option>
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((c) => (
                  <option key={c} value={c}>
                    {toRoman(c)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("yearFilter")}
              </label>
              <YearRangeFilter
                yearMin={filterYearMin}
                yearMax={filterYearMax}
                onChange={(min, max) => {
                  setFilterYearMin(min);
                  setFilterYearMax(max);
                  if (min !== undefined || max !== undefined) setCentury(undefined);
                  setPage(1);
                }}
                disabled={century !== undefined}
              />
            </div>
          </div>
        </div>

        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            {t("loadingManuscripts")}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        {data && data.length > 0 && (
          <div className="rounded-xl border border-border bg-card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-border bg-muted/50">
                    <th className="px-4 py-3 text-left text-sm font-medium">{t("gaId")}</th>
                    <th className="hidden px-4 py-3 text-left text-sm font-medium md:table-cell">{t("name")}</th>
                    <th className="px-4 py-3 text-left text-sm font-medium">{t("century")}</th>
                    <th className="hidden px-4 py-3 text-left text-sm font-medium md:table-cell">{t("type")}</th>
                    <th className="px-4 py-3 text-right text-sm font-medium">{t("books")}</th>
                    <th className="px-4 py-3 text-right text-sm font-medium">{t("verses")}</th>
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((m) => (
                    <tr
                      key={m.gaId}
                      className="border-b border-border last:border-0 hover:bg-muted/30"
                    >
                      <td className="px-4 py-3 font-mono font-medium">{m.gaId}</td>
                      <td className="hidden px-4 py-3 text-sm md:table-cell">{m.name ?? "—"}</td>
                      <td className="px-4 py-3 text-sm">
                        {m.centuryMin === m.centuryMax
                          ? toRoman(m.centuryMin)
                          : `${toRoman(m.centuryMin)}/${toRoman(m.centuryMax)}`}
                        {m.yearBest
                          ? ` (c. ${m.yearBest})`
                          : m.yearMin
                            ? ` (${m.yearMin}–${m.yearMax})`
                            : ""}
                      </td>
                      <td className="hidden px-4 py-3 text-sm capitalize md:table-cell">{m.manuscriptType ?? "—"}</td>
                      <td className="px-4 py-3 text-right">{m.bookCount}</td>
                      <td className="px-4 py-3 text-right">{m.verseCount.toLocaleString()}</td>
                      <td className="px-4 py-3">
                        <Link
                          href={`/manuscripts/${m.gaId}`}
                          className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-sm font-medium text-primary hover:bg-primary/10"
                        >
                          {tc("view")} <ExternalLink className="h-3 w-3" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex justify-between items-center px-4 py-3 border-t border-border bg-muted/30">
              <span className="text-sm text-muted-foreground">
                {t("page", { page, count: data.length })}
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page <= 1}
                  className="rounded-lg px-3 py-1.5 text-sm font-medium bg-secondary disabled:opacity-50"
                >
                  {tc("previous")}
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.length < 50}
                  className="rounded-lg px-3 py-1.5 text-sm font-medium bg-secondary disabled:opacity-50"
                >
                  {tc("next")}
                </button>
              </div>
            </div>
          </div>
        )}

        {data && data.length === 0 && !isLoading && (
          <div className="rounded-xl border border-border bg-card p-12 text-center text-muted-foreground">
            {t("noManuscripts")}
          </div>
        )}
      </div>
    </div>
  );
}
