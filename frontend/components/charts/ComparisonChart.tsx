"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { toRoman } from "@/lib/utils";
import type { TimelineEntry } from "@/types";

interface ComparisonSeries {
  label: string;
  entries: TimelineEntry[];
  color: string;
}

interface ComparisonChartProps {
  series: ComparisonSeries[];
}

const COLORS = ["#3b82f6", "#ef4444", "#10b981", "#f59e0b", "#8b5cf6"];

export function ComparisonChart({ series }: ComparisonChartProps) {
  const allCenturies = new Set<number>();
  series.forEach((s) => s.entries.forEach((e) => allCenturies.add(e.century)));

  const data = Array.from(allCenturies)
    .sort()
    .map((century) => {
      const point: Record<string, number | string> = {
        name: toRoman(century),
        century,
      };
      series.forEach((s) => {
        const entry = s.entries.find((e) => e.century === century);
        point[s.label] = entry?.cumulativePercent ?? 0;
      });
      return point;
    });

  return (
    <ResponsiveContainer width="100%" height={400}>
      <LineChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
        <XAxis dataKey="name" tick={{ fontSize: 12 }} />
        <YAxis
          domain={[0, 100]}
          tick={{ fontSize: 12 }}
          tickFormatter={(v) => `${v}%`}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "var(--card)",
            border: "1px solid var(--border)",
            borderRadius: "8px",
          }}
          formatter={(value) => [`${Number(value).toFixed(2)}%`]}
        />
        <Legend />
        {series.map((s, i) => (
          <Line
            key={s.label}
            type="monotone"
            dataKey={s.label}
            stroke={s.color || COLORS[i % COLORS.length]}
            strokeWidth={2}
            dot={{ r: 4 }}
            activeDot={{ r: 6 }}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );
}
