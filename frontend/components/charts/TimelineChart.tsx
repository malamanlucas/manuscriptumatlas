"use client";

import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Bar,
  BarChart,
  Legend,
} from "recharts";
import { toRoman } from "@/lib/utils";
import { useTranslations } from "next-intl";
import type { TimelineEntry } from "@/types";

interface TimelineChartProps {
  entries: TimelineEntry[];
  showDelta?: boolean;
}

export function TimelineChart({
  entries,
  showDelta = false,
}: TimelineChartProps) {
  const t = useTranslations("charts");

  const data = entries.map((e) => ({
    ...e,
    name: toRoman(e.century),
  }));

  if (showDelta) {
    return (
      <ResponsiveContainer width="100%" height={350}>
        <BarChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
          <XAxis dataKey="name" tick={{ fontSize: 12 }} />
          <YAxis tick={{ fontSize: 12 }} />
          <Tooltip
            contentStyle={{
              backgroundColor: "var(--card)",
              border: "1px solid var(--border)",
              borderRadius: "8px",
            }}
            formatter={(value, name) => {
              const v = Number(value);
              if (name === "newVersesCount") return [v, t("newVerses")];
              if (name === "growthPercent")
                return [`${v.toFixed(2)}%`, t("growth")];
              return [v, String(name)];
            }}
          />
          <Legend />
          <Bar
            dataKey="newVersesCount"
            name={t("newVerses")}
            fill="#4a6fa5"
            radius={[4, 4, 0, 0]}
          />
        </BarChart>
      </ResponsiveContainer>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={350}>
      <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id="colorCoverage" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#4a6fa5" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#4a6fa5" stopOpacity={0} />
          </linearGradient>
        </defs>
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
          formatter={(value) => [`${Number(value).toFixed(2)}%`, t("coverage")]}
        />
        <Area
          type="monotone"
          dataKey="cumulativePercent"
          stroke="#4a6fa5"
          strokeWidth={2}
          fillOpacity={1}
          fill="url(#colorCoverage)"
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
