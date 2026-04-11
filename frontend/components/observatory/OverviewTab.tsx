"use client";

import { useTranslations } from "next-intl";
import {
  useAnalyticsOverview,
  useAnalyticsLive,
  useAnalyticsDistribution,
  useAnalyticsTopPages,
  useAnalyticsTopReferrers,
  useAnalyticsTimelineSessions,
} from "@/hooks/useVisitorAnalytics";
import type { SessionFilters } from "@/types";
import {
  Activity,
  Users,
  Eye,
  Timer,
  Monitor,
  Globe,
  Smartphone,
} from "lucide-react";
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  BarChart,
  Bar,
} from "recharts";

const COLORS = [
  "#4a6fa5",
  "#5a8a7a",
  "#b8976a",
  "#a65d57",
  "#7a6e8a",
  "#9e7b8a",
  "#5e8e9e",
  "#7a8e5a",
];

export function OverviewTab({ filters }: { filters: SessionFilters }) {
  const t = useTranslations("observatory");
  const { data: overview } = useAnalyticsOverview(filters);
  const { data: live } = useAnalyticsLive();
  const { data: browserDist } = useAnalyticsDistribution("browser", filters);
  const { data: osDist } = useAnalyticsDistribution("os", filters);
  const { data: deviceDist } = useAnalyticsDistribution("device", filters);
  const { data: topPages } = useAnalyticsTopPages(filters);
  const { data: topReferrers } = useAnalyticsTopReferrers(filters);
  const { data: timeline } = useAnalyticsTimelineSessions({
    ...filters,
    granularity: "hour",
  });

  const timelineData =
    timeline?.buckets.map((b) => ({
      time: new Date(b.bucket).toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      }),
      count: b.count,
    })) ?? [];

  return (
    <div className="space-y-6">
      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <KPICard
          icon={Activity}
          label={t("kpi.activeNow")}
          value={overview?.activeNow ?? 0}
          pulse
        />
        <KPICard
          icon={Eye}
          label={t("kpi.sessions")}
          value={overview?.sessionsInRange ?? 0}
        />
        <KPICard
          icon={Users}
          label={t("kpi.uniqueVisitors")}
          value={overview?.uniqueVisitorsInRange ?? 0}
        />
        <KPICard
          icon={Timer}
          label={t("kpi.avgLoadTime")}
          value={
            overview?.avgLoadTimeMs
              ? `${(overview.avgLoadTimeMs / 1000).toFixed(1)}s`
              : "—"
          }
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Live Visitors Table */}
        <div className="rounded-xl border border-border bg-card p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold">{t("liveVisitors")}</h3>
            <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <span className="inline-block h-2 w-2 animate-pulse rounded-full bg-green-500" />
              {t("autoRefresh")}
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-border text-left text-muted-foreground">
                  <th className="pb-2 pr-3">IP</th>
                  <th className="pb-2 pr-3">{t("table.browser")}</th>
                  <th className="pb-2 pr-3">OS</th>
                  <th className="pb-2 pr-3">{t("table.page")}</th>
                </tr>
              </thead>
              <tbody>
                {(live ?? []).slice(0, 10).map((v) => (
                  <tr
                    key={v.sessionId}
                    className="border-b border-border/50 last:border-0"
                  >
                    <td className="py-1.5 pr-3 font-mono">{v.ipAddress}</td>
                    <td className="py-1.5 pr-3">{v.browserName ?? "—"}</td>
                    <td className="py-1.5 pr-3">{v.osName ?? "—"}</td>
                    <td className="py-1.5 pr-3 truncate max-w-[120px]">
                      {v.currentPage ?? "—"}
                    </td>
                  </tr>
                ))}
                {(!live || live.length === 0) && (
                  <tr>
                    <td colSpan={4} className="py-4 text-center text-muted-foreground">
                      {t("noActiveVisitors")}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Sessions per hour chart */}
        <div className="rounded-xl border border-border bg-card p-4">
          <h3 className="mb-3 text-sm font-semibold">
            {t("sessionsPerHour")}
          </h3>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={timelineData}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis dataKey="time" tick={{ fontSize: 10 }} />
              <YAxis tick={{ fontSize: 10 }} />
              <Tooltip />
              <Area
                type="monotone"
                dataKey="count"
                stroke="#4a6fa5"
                fill="#4a6fa5"
                fillOpacity={0.2}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Distribution Charts */}
      <div className="grid gap-6 md:grid-cols-3">
        <DistributionPie
          title={t("dist.browsers")}
          icon={Globe}
          data={browserDist?.items ?? []}
        />
        <DistributionPie
          title={t("dist.os")}
          icon={Monitor}
          data={osDist?.items ?? []}
        />
        <DistributionPie
          title={t("dist.devices")}
          icon={Smartphone}
          data={deviceDist?.items ?? []}
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Top Pages */}
        <div className="rounded-xl border border-border bg-card p-4">
          <h3 className="mb-3 text-sm font-semibold">{t("topPages")}</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart
              data={(topPages ?? []).slice(0, 10)}
              layout="vertical"
              margin={{ left: 80 }}
            >
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis type="number" tick={{ fontSize: 10 }} />
              <YAxis
                dataKey="path"
                type="category"
                tick={{ fontSize: 10 }}
                width={80}
              />
              <Tooltip />
              <Bar dataKey="count" fill="#4a6fa5" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Top Referrers */}
        <div className="rounded-xl border border-border bg-card p-4">
          <h3 className="mb-3 text-sm font-semibold">{t("topReferrers")}</h3>
          <div className="space-y-2">
            {(topReferrers ?? []).slice(0, 8).map((r, i) => (
              <div key={r.referrer} className="flex items-center justify-between text-sm">
                <span className="truncate max-w-[200px]">{r.referrer}</span>
                <span className="font-mono text-muted-foreground">
                  {r.count}
                </span>
              </div>
            ))}
            {(!topReferrers || topReferrers.length === 0) && (
              <p className="text-sm text-muted-foreground">{t("noData")}</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function KPICard({
  icon: Icon,
  label,
  value,
  pulse,
}: {
  icon: React.ElementType;
  label: string;
  value: number | string;
  pulse?: boolean;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="flex items-center gap-2 text-muted-foreground">
        <Icon className="h-4 w-4" />
        <span className="text-xs font-medium">{label}</span>
      </div>
      <div className="mt-2 flex items-center gap-2">
        {pulse && (
          <span className="inline-block h-2 w-2 animate-pulse rounded-full bg-green-500" />
        )}
        <span className="text-2xl font-bold">{value}</span>
      </div>
    </div>
  );
}

function DistributionPie({
  title,
  icon: Icon,
  data,
}: {
  title: string;
  icon: React.ElementType;
  data: { value: string; count: number; percent: number }[];
}) {
  const chartData = data.map((d) => ({ name: d.value, value: d.count }));

  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="mb-3 flex items-center gap-2">
        <Icon className="h-4 w-4 text-muted-foreground" />
        <h3 className="text-sm font-semibold">{title}</h3>
      </div>
      {chartData.length > 0 ? (
        <ResponsiveContainer width="100%" height={180}>
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={40}
              outerRadius={70}
              dataKey="value"
              paddingAngle={2}
            >
              {chartData.map((_, i) => (
                <Cell key={i} fill={COLORS[i % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      ) : (
        <div className="flex h-[180px] items-center justify-center text-sm text-muted-foreground">
          No data
        </div>
      )}
      <div className="mt-2 space-y-1">
        {data.slice(0, 5).map((d, i) => (
          <div key={d.value} className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              <span
                className="inline-block h-2 w-2 rounded-full"
                style={{ backgroundColor: COLORS[i % COLORS.length] }}
              />
              <span>{d.value}</span>
            </div>
            <span className="text-muted-foreground">
              {d.percent.toFixed(1)}%
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
