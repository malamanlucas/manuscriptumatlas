"use client";

import { Link } from "@/i18n/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useNtMetrics } from "@/hooks/useMetrics";
import { toRoman } from "@/lib/utils";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import { coverageHeatColor } from "@/lib/utils";

export default function MetricsPage() {
  const t = useTranslations("metrics");
  const tc = useTranslations("common");
  const tBooks = useTranslations("books");

  const { data, isLoading, error } = useNtMetrics();

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            {t("loadingMetrics")}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        {data && (
          <>
            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <h3 className="text-base font-semibold mb-4">{t("overallStabilization")}</h3>
              <p className="text-muted-foreground">
                {t("stabilizationDescription", {
                  century: data.overallStabilizationCentury != null
                    ? toRoman(data.overallStabilizationCentury)
                    : "—",
                })}
              </p>
            </div>

            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <h3 className="text-base font-semibold mb-4">{t("coverageByCentury")}</h3>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={data.overallCoverageByCentury}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis
                      dataKey="century"
                      tickFormatter={(c) => toRoman(c)}
                    />
                    <YAxis
                      domain={[0, 100]}
                      tickFormatter={(v) => `${v}%`}
                    />
                    <Tooltip
                      formatter={(v: number | undefined) => [v != null ? `${v.toFixed(1)}%` : "—", tc("coverage")]}
                      labelFormatter={(c) => `${tc("century")} ${toRoman(c)}`}
                    />
                    <Bar dataKey="percent" name={tc("coverage")} radius={[4, 4, 0, 0]}>
                      {data.overallCoverageByCentury.map((entry, i) => (
                        <Cell
                          key={i}
                          fill={coverageHeatColor(entry.percent)}
                        />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="rounded-xl border border-border bg-card overflow-hidden">
              <h3 className="text-base font-semibold p-4 pb-0 md:p-6 md:pb-0">{t("metricsByBook")}</h3>
              <div className="overflow-x-auto p-4 md:p-6">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="px-4 py-3 text-left text-sm font-medium">{tc("book")}</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">{t("stabilization")}</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">{t("fragmentation")}</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">{t("coverageDensity")}</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">{t("concentration")}</th>
                      <th className="px-4 py-3"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.books.map((b) => (
                      <tr
                        key={b.bookName}
                        className="border-b border-border last:border-0 hover:bg-muted/30"
                      >
                        <td className="px-4 py-3 font-medium">{tBooks(b.bookName)}</td>
                        <td className="px-4 py-3 text-right">
                          {b.stabilizationCentury != null
                            ? toRoman(b.stabilizationCentury)
                            : "—"}
                        </td>
                        <td className="px-4 py-3 text-right">
                          {b.fragmentationIndex.toFixed(3)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          {b.coverageDensity.toFixed(1)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          {(b.manuscriptConcentrationScore * 100).toFixed(1)}%
                        </td>
                        <td className="px-4 py-3">
                          <Link
                            href={`/metrics/${encodeURIComponent(b.bookName)}`}
                            className="text-sm text-primary hover:underline"
                          >
                            {tc("details")}
                          </Link>
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
