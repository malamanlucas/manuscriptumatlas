"use client";

import { useParams } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useBookMetrics } from "@/hooks/useMetrics";
import { toRoman } from "@/lib/utils";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { ArrowLeft } from "lucide-react";

export default function BookMetricsPage() {
  const t = useTranslations("bookMetrics");
  const tc = useTranslations("common");
  const tBooks = useTranslations("books");

  const params = useParams();
  const book = params.book as string;
  const decodedBook = decodeURIComponent(book);

  const { data, isLoading, error } = useBookMetrics(decodedBook);

  return (
    <div className="min-h-screen">
      <Header
        title={tBooks(decodedBook)}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
        <Link
          href="/metrics"
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" /> {t("backToMetrics")}
        </Link>

        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            {tc("loading")}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        {data && (
          <div className="space-y-6">
            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <h3 className="text-base font-semibold mb-4">{t("keyMetrics")}</h3>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                <div>
                  <p className="text-xs text-muted-foreground">{t("stabilizationCentury")}</p>
                  <p className="text-xl font-bold">
                    {data.stabilizationCentury != null
                      ? toRoman(data.stabilizationCentury)
                      : "—"}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("fragmentationIndex")}</p>
                  <p className="text-xl font-bold">{data.fragmentationIndex.toFixed(3)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("coverageDensity")}</p>
                  <p className="text-xl font-bold">{data.coverageDensity.toFixed(1)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("manuscriptConcentration")}</p>
                  <p className="text-xl font-bold">
                    {(data.manuscriptConcentrationScore * 100).toFixed(1)}%
                  </p>
                </div>
              </div>
            </div>

            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <h3 className="text-base font-semibold mb-4">{t("coverageByCentury")}</h3>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={data.coverageByCentury}>
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
                    <Line
                      type="monotone"
                      dataKey="percent"
                      stroke="hsl(var(--primary))"
                      strokeWidth={2}
                      dot={{ r: 4 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <h3 className="text-base font-semibold mb-4">{t("centuryGrowthRate")}</h3>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="px-4 py-2 text-left text-sm font-medium">{tc("century")}</th>
                      <th className="px-4 py-2 text-right text-sm font-medium">{t("growthRate")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.centuryGrowthRates.map((r) => (
                      <tr key={r.century} className="border-b border-border last:border-0">
                        <td className="px-4 py-2">{toRoman(r.century)}</td>
                        <td className="px-4 py-2 text-right">
                          {r.rate >= 0 ? "+" : ""}{r.rate.toFixed(1)}%
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
