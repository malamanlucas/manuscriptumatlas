"use client";

import { useState, useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  useAnalyticsTimelineSessions,
  useAnalyticsTimelinePageviews,
  useAnalyticsHeatmap,
} from "@/hooks/useVisitorAnalytics";
import type { SessionFilters } from "@/types";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";

const COLORS = [
  "#3b82f6",
  "#10b981",
  "#f59e0b",
  "#ef4444",
  "#8b5cf6",
  "#ec4899",
  "#06b6d4",
  "#84cc16",
];

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

export function TimelineTab({ filters }: { filters: SessionFilters }) {
  const t = useTranslations("observatory");
  const [metric, setMetric] = useState<"sessions" | "pageviews">("sessions");
  const [breakdown, setBreakdown] = useState<string>("none");
  const [granularity, setGranularity] = useState<string | undefined>();

  const timelineQuery = {
    ...filters,
    granularity,
    breakdown: breakdown === "none" ? undefined : breakdown,
  };

  const { data: sessionTimeline } =
    metric === "sessions"
      ? useAnalyticsTimelineSessions(timelineQuery)
      : { data: undefined };

  const { data: pageviewTimeline } =
    metric === "pageviews"
      ? useAnalyticsTimelinePageviews(timelineQuery)
      : { data: undefined };

  const timeline = metric === "sessions" ? sessionTimeline : pageviewTimeline;

  const { data: heatmap } = useAnalyticsHeatmap(filters);

  const chartData = useMemo(() => {
    if (!timeline?.buckets) return [];
    return timeline.buckets.map((b) => ({
      time: new Date(b.bucket).toLocaleString([], {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }),
      count: b.count,
      ...(b.series ?? {}),
    }));
  }, [timeline]);

  const seriesKeys = useMemo(() => {
    if (!timeline?.buckets || breakdown === "none") return [];
    const keys = new Set<string>();
    timeline.buckets.forEach((b) => {
      if (b.series) Object.keys(b.series).forEach((k) => keys.add(k));
    });
    return Array.from(keys).slice(0, 8);
  }, [timeline, breakdown]);

  const heatmapGrid = useMemo(() => {
    if (!heatmap?.cells) return [];
    const grid: number[][] = Array.from({ length: 7 }, () =>
      Array(24).fill(0)
    );
    let max = 0;
    heatmap.cells.forEach((c) => {
      grid[c.dayOfWeek][c.hourOfDay] = c.count;
      if (c.count > max) max = c.count;
    });
    return { grid, max };
  }, [heatmap]);

  return (
    <div className="space-y-6">
      {/* Controls */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex rounded-lg border border-border bg-card p-1">
          {(["sessions", "pageviews"] as const).map((m) => (
            <button
              key={m}
              onClick={() => setMetric(m)}
              className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                metric === m
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-muted"
              }`}
            >
              {m === "sessions" ? t("timeline.sessions") : t("timeline.pageviews")}
            </button>
          ))}
        </div>

        <select
          value={granularity ?? ""}
          onChange={(e) => setGranularity(e.target.value || undefined)}
          className="rounded-lg border border-border bg-card px-3 py-1.5 text-xs"
        >
          <option value="">Auto</option>
          <option value="minute">Minute</option>
          <option value="hour">Hour</option>
          <option value="day">Day</option>
        </select>

        <select
          value={breakdown}
          onChange={(e) => setBreakdown(e.target.value)}
          className="rounded-lg border border-border bg-card px-3 py-1.5 text-xs"
        >
          <option value="none">{t("timeline.noBreakdown")}</option>
          <option value="browser">{t("table.browser")}</option>
          <option value="os">OS</option>
          <option value="device">{t("table.device")}</option>
        </select>
      </div>

      {/* Main chart */}
      <div className="rounded-xl border border-border bg-card p-4">
        <h3 className="mb-3 text-sm font-semibold">
          {metric === "sessions"
            ? t("timeline.sessionsChart")
            : t("timeline.pageviewsChart")}
        </h3>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
            <XAxis dataKey="time" tick={{ fontSize: 10 }} />
            <YAxis tick={{ fontSize: 10 }} />
            <Tooltip />
            {breakdown === "none" ? (
              <Area
                type="monotone"
                dataKey="count"
                stroke="#3b82f6"
                fill="#3b82f6"
                fillOpacity={0.2}
              />
            ) : (
              <>
                {seriesKeys.map((key, i) => (
                  <Area
                    key={key}
                    type="monotone"
                    dataKey={key}
                    stackId="1"
                    stroke={COLORS[i % COLORS.length]}
                    fill={COLORS[i % COLORS.length]}
                    fillOpacity={0.4}
                  />
                ))}
                <Legend />
              </>
            )}
          </AreaChart>
        </ResponsiveContainer>
      </div>

      {/* Heatmap */}
      <div className="rounded-xl border border-border bg-card p-4">
        <h3 className="mb-3 text-sm font-semibold">{t("timeline.heatmap")}</h3>
        {heatmapGrid && "grid" in heatmapGrid ? (
          <div className="overflow-x-auto">
            <div className="min-w-[600px]">
              {/* Hour headers */}
              <div className="flex">
                <div className="w-12" />
                {Array.from({ length: 24 }, (_, h) => (
                  <div
                    key={h}
                    className="flex-1 text-center text-[10px] text-muted-foreground"
                  >
                    {h.toString().padStart(2, "0")}
                  </div>
                ))}
              </div>
              {/* Grid rows */}
              {heatmapGrid.grid.map((row, dow) => (
                <div key={dow} className="flex items-center">
                  <div className="w-12 text-xs text-muted-foreground">
                    {DAYS[dow]}
                  </div>
                  {row.map((count, hour) => {
                    const intensity =
                      heatmapGrid.max > 0
                        ? Math.round((count / heatmapGrid.max) * 100)
                        : 0;
                    return (
                      <div
                        key={hour}
                        className="flex-1 aspect-square m-0.5 rounded-sm"
                        style={{
                          backgroundColor: `hsl(217, 91%, ${100 - intensity * 0.5}%)`,
                        }}
                        title={`${DAYS[dow]} ${hour}:00 — ${count}`}
                      />
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">{t("noData")}</p>
        )}
      </div>
    </div>
  );
}
