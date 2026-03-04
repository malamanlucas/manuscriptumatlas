"use client";

import { useState, useMemo } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { AuthGate } from "@/components/observatory/AuthGate";
import { Telescope, Eye, Search, Clock, Users } from "lucide-react";
import { OverviewTab } from "@/components/observatory/OverviewTab";
import { ExplorerTab } from "@/components/observatory/ExplorerTab";
import { TimelineTab } from "@/components/observatory/TimelineTab";
import { VisitorsTab } from "@/components/observatory/VisitorsTab";
import type { SessionFilters } from "@/types";

const TIME_PRESETS = [
  { label: "15m", value: 15 * 60 * 1000 },
  { label: "1h", value: 60 * 60 * 1000 },
  { label: "6h", value: 6 * 60 * 60 * 1000 },
  { label: "24h", value: 24 * 60 * 60 * 1000 },
  { label: "7d", value: 7 * 24 * 60 * 60 * 1000 },
  { label: "30d", value: 30 * 24 * 60 * 60 * 1000 },
  { label: "90d", value: 90 * 24 * 60 * 60 * 1000 },
];

type Tab = "overview" | "explorer" | "timeline" | "visitors";

export default function ObservatoryPage() {
  return (
    <AuthGate requiredRole="ADMIN">
      <ObservatoryContent />
    </AuthGate>
  );
}

function ObservatoryContent() {
  const t = useTranslations("observatory");
  const [activeTab, setActiveTab] = useState<Tab>("overview");
  const [timePreset, setTimePreset] = useState(TIME_PRESETS[4]);

  const timeFilters: SessionFilters = useMemo(() => {
    const now = new Date();
    const from = new Date(now.getTime() - timePreset.value);
    return {
      from: from.toISOString(),
      to: now.toISOString(),
    };
  }, [timePreset]);

  const tabs: { id: Tab; label: string; icon: React.ElementType }[] = [
    { id: "overview", label: t("tabs.overview"), icon: Eye },
    { id: "explorer", label: t("tabs.explorer"), icon: Search },
    { id: "timeline", label: t("tabs.timeline"), icon: Clock },
    { id: "visitors", label: t("tabs.visitors"), icon: Users },
  ];

  return (
    <div className="min-h-screen bg-background">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6">
        {/* Time range + Tab bar */}
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2">
            <Telescope className="h-5 w-5 text-primary" />
            <h2 className="text-lg font-semibold">{t("title")}</h2>
          </div>

          {/* Time presets */}
          <div className="flex flex-wrap items-center gap-1 rounded-lg border border-border bg-card p-1">
            {TIME_PRESETS.map((preset) => (
              <button
                key={preset.label}
                onClick={() => setTimePreset(preset)}
                className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                  timePreset.label === preset.label
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted"
                }`}
              >
                {preset.label}
              </button>
            ))}
          </div>
        </div>

        {/* Tabs */}
        <div className="mb-6 flex gap-1 overflow-x-auto rounded-lg border border-border bg-card p-1">
          {tabs.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setActiveTab(id)}
              className={`flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors whitespace-nowrap ${
                activeTab === id
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-muted"
              }`}
            >
              <Icon className="h-4 w-4" />
              {label}
            </button>
          ))}
        </div>

        {/* Tab content */}
        {activeTab === "overview" && <OverviewTab filters={timeFilters} />}
        {activeTab === "explorer" && <ExplorerTab filters={timeFilters} />}
        {activeTab === "timeline" && <TimelineTab filters={timeFilters} />}
        {activeTab === "visitors" && <VisitorsTab filters={timeFilters} />}
      </div>
    </div>
  );
}
