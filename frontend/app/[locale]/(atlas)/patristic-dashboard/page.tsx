"use client";

import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { usePatristicStats } from "@/hooks/useChurchFathers";
import { useCouncilTypeSummary } from "@/hooks/useCouncils";
import {
  Users,
  Quote,
  Landmark,
  Calendar,
  Globe,
  BookOpen,
  BarChart3,
  Loader2,
  Crown,
  MapPin,
  Building2,
} from "lucide-react";

const TRADITION_COLORS: Record<string, string> = {
  greek: "bg-[#4a6fa5]",
  latin: "bg-[#b8976a]",
  syriac: "bg-[#5a8a7a]",
  coptic: "bg-[#7a6e8a]",
};

const COUNCIL_ICONS: Record<string, React.ElementType> = {
  ECUMENICAL: Crown,
  REGIONAL: MapPin,
  LOCAL: Building2,
};

const COUNCIL_COLORS: Record<string, { bg: string; icon: string }> = {
  ECUMENICAL: { bg: "bg-[#b8976a]/15 dark:bg-[#b8976a]/20", icon: "text-[#b8976a]" },
  REGIONAL: { bg: "bg-[#4a6fa5]/15 dark:bg-[#4a6fa5]/20", icon: "text-[#4a6fa5]" },
  LOCAL: { bg: "bg-[#5a8a7a]/15 dark:bg-[#5a8a7a]/20", icon: "text-[#5a8a7a]" },
};

function toRoman(n: number): string {
  const map: [number, string][] = [
    [10, "X"], [9, "IX"], [5, "V"], [4, "IV"], [1, "I"],
  ];
  let result = "";
  let val = n;
  for (const [value, numeral] of map) {
    while (val >= value) { result += numeral; val -= value; }
  }
  return result;
}

export default function PatristicDashboardPage() {
  const t = useTranslations("patristicDashboard");
  const tc = useTranslations("common");
  const { data, isLoading, error } = usePatristicStats();
  const { data: councilTypes } = useCouncilTypeSummary();

  const totalCouncils = councilTypes?.reduce((sum, c) => sum + c.count, 0) ?? 0;
  const datingPct = data ? Math.round((data.totalWithDating / data.totalFathers) * 100) : 0;

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
        {/* Loading skeleton */}
        {isLoading && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="animate-pulse rounded-xl border border-border bg-card p-5">
                  <div className="flex items-center gap-3">
                    <div className="h-9 w-9 rounded-lg bg-secondary" />
                    <div className="space-y-2">
                      <div className="h-6 w-12 rounded bg-secondary" />
                      <div className="h-3 w-20 rounded bg-secondary" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {[1, 2].map((i) => (
                <div key={i} className="animate-pulse rounded-xl border border-border bg-card p-5">
                  <div className="h-4 w-28 rounded bg-secondary mb-4" />
                  <div className="space-y-3">
                    {[1, 2, 3].map((j) => (
                      <div key={j} className="h-6 rounded bg-secondary" />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        {data && (
          <>
            {/* Global Stats Cards */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-4">
              <StatCard
                icon={Users}
                iconBg="bg-[#4a6fa5]/15 dark:bg-[#4a6fa5]/20"
                iconColor="text-[#4a6fa5]"
                value={data.totalFathers}
                label={t("totalFathers")}
              />
              <StatCard
                icon={Quote}
                iconBg="bg-[#5a8a7a]/15 dark:bg-[#5a8a7a]/20"
                iconColor="text-[#5a8a7a]"
                value={data.totalStatements}
                label={t("totalStatements")}
              />
              <StatCard
                icon={Landmark}
                iconBg="bg-[#7a6e8a]/15 dark:bg-[#7a6e8a]/20"
                iconColor="text-[#7a6e8a]"
                value={totalCouncils}
                label={t("totalCouncils")}
              />
              {/* Dating coverage with inline progress */}
              <div className="rounded-xl border border-border bg-card p-5">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-[#b8976a]/15 p-2 dark:bg-[#b8976a]/20">
                    <Calendar className="h-5 w-5 text-[#b8976a]" />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-baseline gap-1.5">
                      <p className="text-2xl font-bold">{datingPct}%</p>
                      <span className="text-xs text-muted-foreground">
                        ({data.totalWithDating}/{data.totalFathers})
                      </span>
                    </div>
                    <p className="text-xs text-muted-foreground">{t("datingCoverage")}</p>
                    <div className="mt-1.5 h-1.5 w-full rounded-full bg-muted">
                      <div
                        className="h-1.5 rounded-full bg-[#b8976a] transition-all"
                        style={{ width: `${datingPct}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Tradition + Century */}
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {/* By Tradition */}
              <div className="rounded-xl border border-border bg-card p-4 md:p-5">
                <h2 className="mb-4 flex items-center gap-2 text-base font-semibold">
                  <Globe className="h-5 w-5 text-muted-foreground" />
                  {t("byTradition")}
                </h2>
                <div className="space-y-4">
                  {Object.entries(data.byTradition)
                    .sort(([, a], [, b]) => b - a)
                    .map(([tradition, count]) => {
                      const pct = (count / data.totalFathers) * 100;
                      return (
                        <div key={tradition}>
                          <div className="mb-1.5 flex items-center justify-between">
                            <span className="flex items-center gap-2 text-sm font-medium">
                              <span className={`h-2.5 w-2.5 rounded-full ${TRADITION_COLORS[tradition] ?? "bg-gray-400"}`} />
                              {t(`traditions.${tradition}`)}
                            </span>
                            <span className="text-sm tabular-nums">
                              <span className="font-semibold">{count}</span>
                              <span className="ml-1 text-xs text-muted-foreground">({pct.toFixed(0)}%)</span>
                            </span>
                          </div>
                          <div className="h-2 w-full rounded-full bg-muted">
                            <div
                              className={`h-2 rounded-full transition-all ${TRADITION_COLORS[tradition] ?? "bg-gray-400"}`}
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                        </div>
                      );
                    })}
                </div>
              </div>

              {/* By Century */}
              <div className="rounded-xl border border-border bg-card p-4 md:p-5">
                <h2 className="mb-4 flex items-center gap-2 text-base font-semibold">
                  <BarChart3 className="h-5 w-5 text-muted-foreground" />
                  {t("byCentury")}
                </h2>
                <div className="space-y-2">
                  {Array.from({ length: 10 }, (_, i) => i + 1).map((c) => {
                    const count = data.byCentury[String(c)] ?? 0;
                    const maxCount = Math.max(...Object.values(data.byCentury), 1);
                    const pct = (count / maxCount) * 100;
                    return (
                      <div key={c} className="flex items-center gap-3">
                        <span className="w-7 text-right text-xs font-medium text-muted-foreground tabular-nums">
                          {toRoman(c)}
                        </span>
                        <div className="h-6 flex-1 rounded-md bg-muted">
                          {count > 0 && (
                            <div
                              className="flex h-6 items-center rounded-md bg-primary/80 px-2 text-[11px] font-medium text-primary-foreground transition-all"
                              style={{ width: `${Math.max(pct, 10)}%` }}
                            >
                              {count}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Topics Distribution */}
            <div className="rounded-xl border border-border bg-card p-4 md:p-5">
              <h2 className="mb-4 flex items-center gap-2 text-base font-semibold">
                <BookOpen className="h-5 w-5 text-muted-foreground" />
                {t("topicsDistribution")}
              </h2>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {data.topicsSummary
                  .sort((a, b) => b.count - a.count)
                  .map((topic) => {
                    const maxCount = Math.max(...data.topicsSummary.map((t) => t.count), 1);
                    const pct = (topic.count / maxCount) * 100;
                    return (
                      <div
                        key={topic.topic}
                        className="rounded-lg border border-border p-3"
                      >
                        <div className="mb-2 flex items-center justify-between">
                          <span className="text-xs font-medium text-muted-foreground">
                            {t(`topics.${topic.topic}`)}
                          </span>
                          <span className="text-lg font-bold tabular-nums">{topic.count}</span>
                        </div>
                        <div className="h-1.5 w-full rounded-full bg-muted">
                          <div
                            className="h-1.5 rounded-full bg-primary/60 transition-all"
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
              </div>
            </div>

            {/* Council Types */}
            {councilTypes && councilTypes.length > 0 && (
              <div className="rounded-xl border border-border bg-card p-4 md:p-5">
                <h2 className="mb-4 flex items-center gap-2 text-base font-semibold">
                  <Landmark className="h-5 w-5 text-muted-foreground" />
                  {t("councilsByType")}
                </h2>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                  {councilTypes.map((ct) => {
                    const IconComp = COUNCIL_ICONS[ct.councilType] ?? Landmark;
                    const colors = COUNCIL_COLORS[ct.councilType] ?? { bg: "bg-muted", icon: "text-muted-foreground" };
                    return (
                      <div key={ct.councilType} className="flex items-center gap-3 rounded-xl border border-border p-4">
                        <div className={`rounded-lg p-2 ${colors.bg}`}>
                          <IconComp className={`h-5 w-5 ${colors.icon}`} />
                        </div>
                        <div>
                          <p className="text-2xl font-bold tabular-nums">{ct.count}</p>
                          <p className="text-xs text-muted-foreground">{t(`councilTypes.${ct.councilType}`)}</p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function StatCard({
  icon: Icon,
  iconBg,
  iconColor,
  value,
  label,
}: {
  icon: React.ElementType;
  iconBg: string;
  iconColor: string;
  value: number | string;
  label: string;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-3">
        <div className={`rounded-lg p-2 ${iconBg}`}>
          <Icon className={`h-5 w-5 ${iconColor}`} />
        </div>
        <div>
          <p className="text-2xl font-bold tabular-nums">
            {typeof value === "number" ? value.toLocaleString() : value}
          </p>
          <p className="text-xs text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  );
}
