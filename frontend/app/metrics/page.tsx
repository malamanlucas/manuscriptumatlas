"use client";

import Link from "next/link";
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
  const { data, isLoading, error } = useNtMetrics();

  return (
    <div className="min-h-screen">
      <Header
        title="Academic Metrics"
        subtitle="Indicators for textual transmission analysis"
      />

      <div className="p-6 space-y-6">
        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            Loading metrics...
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            Failed to load: {(error as Error).message}
          </div>
        )}

        {data && (
          <>
            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="text-base font-semibold mb-4">Overall NT Stabilization</h3>
              <p className="text-muted-foreground">
                The New Testament reached 90% coverage by century{" "}
                <span className="font-bold text-foreground">
                  {data.overallStabilizationCentury != null
                    ? toRoman(data.overallStabilizationCentury)
                    : "—"}
                </span>
              </p>
            </div>

            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="text-base font-semibold mb-4">Coverage by Century (NT)</h3>
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
                      formatter={(v: number | undefined) => [v != null ? `${v.toFixed(1)}%` : "—", "Coverage"]}
                      labelFormatter={(c) => `Century ${toRoman(c)}`}
                    />
                    <Bar dataKey="percent" name="Coverage" radius={[4, 4, 0, 0]}>
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
              <h3 className="text-base font-semibold p-6 pb-0">Metrics by Book</h3>
              <div className="overflow-x-auto p-6">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="px-4 py-3 text-left text-sm font-medium">Book</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">Stabilization</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">Fragmentation</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">Coverage Density</th>
                      <th className="px-4 py-3 text-right text-sm font-medium">Concentration</th>
                      <th className="px-4 py-3"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.books.map((b) => (
                      <tr
                        key={b.bookName}
                        className="border-b border-border last:border-0 hover:bg-muted/30"
                      >
                        <td className="px-4 py-3 font-medium">{b.bookName}</td>
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
                            Details
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
