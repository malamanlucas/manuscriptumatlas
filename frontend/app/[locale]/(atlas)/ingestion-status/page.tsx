"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { AuthGate } from "@/components/observatory/AuthGate";
import {
  useIngestionStatus,
  useTriggerIngestion,
} from "@/hooks/useIngestion";
import {
  Loader2,
  CheckCircle,
  XCircle,
  Clock,
  Play,
  FileText,
  Link2,
  Timer,
  ScrollText,
  BookOpen,
  Church,
  Cpu,
} from "lucide-react";
import { ManuscriptIngestionPanel } from "@/components/ingestion/ManuscriptIngestionPanel";
import { PatristicIngestionPanel } from "@/components/ingestion/PatristicIngestionPanel";
import { CouncilIngestionPanel } from "@/components/ingestion/CouncilIngestionPanel";
import { BibleIngestionPanel } from "@/components/ingestion/BibleIngestionPanel";
import { LlmQueuePanel } from "@/components/ingestion/LlmQueuePanel";

type DomainTab = "manuscripts" | "patristic" | "councils" | "bible" | "llmQueue";

export default function IngestionStatusPage() {
  return (
    <AuthGate requiredRole="ADMIN">
      <IngestionContent />
    </AuthGate>
  );
}

function IngestionContent() {
  const t = useTranslations("ingestion");
  const tc = useTranslations("common");
  const { data, isLoading, error } = useIngestionStatus();
  const trigger = useTriggerIngestion();
  const [activeTab, setActiveTab] = useState<DomainTab>("manuscripts");

  const statusKey = data?.isRunning ? "running" : (data?.status ?? "idle");

  const STATUS_ICON: Record<string, React.ReactNode> = {
    idle: <Clock className="h-4 w-4 text-muted-foreground" />,
    running: <Loader2 className="h-4 w-4 animate-spin text-blue-500" />,
    success: <CheckCircle className="h-4 w-4 text-emerald-500" />,
    failed: <XCircle className="h-4 w-4 text-red-500" />,
  };

  const TABS: { key: DomainTab; icon: React.ReactNode }[] = [
    { key: "manuscripts", icon: <ScrollText className="h-4 w-4" /> },
    { key: "patristic", icon: <BookOpen className="h-4 w-4" /> },
    { key: "councils", icon: <Church className="h-4 w-4" /> },
    { key: "bible", icon: <BookOpen className="h-4 w-4" /> },
    { key: "llmQueue", icon: <Cpu className="h-4 w-4" /> },
  ];

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-5xl p-4 md:p-6 space-y-5">
        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            {t("loadingStatus")}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        {data && (
          <>
            {/* Compact status bar + metrics */}
            <div className="rounded-xl border border-border bg-card p-4">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-2">
                    {STATUS_ICON[statusKey] ?? STATUS_ICON.idle}
                    <span className="text-sm font-medium">{t(statusKey as "idle")}</span>
                  </div>
                  <div className="hidden sm:flex items-center gap-4 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1.5">
                      <Timer className="h-3.5 w-3.5" />
                      {data.durationMs != null ? formatDuration(data.durationMs) : "—"}
                    </span>
                    <span className="flex items-center gap-1.5">
                      <FileText className="h-3.5 w-3.5" />
                      {data.manuscriptsIngested.toLocaleString()}
                    </span>
                    <span className="flex items-center gap-1.5">
                      <Link2 className="h-3.5 w-3.5" />
                      {data.versesLinked.toLocaleString()}
                    </span>
                  </div>
                </div>
                <button
                  onClick={() => trigger.mutate()}
                  disabled={data.isRunning || trigger.isPending || !data.enableIngestion}
                  className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {trigger.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Play className="h-4 w-4" />
                  )}
                  {t("runNow")}
                </button>
              </div>

              {/* Mobile metrics */}
              <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground sm:hidden">
                <span className="flex items-center gap-1.5">
                  <Timer className="h-3.5 w-3.5" />
                  {data.durationMs != null ? formatDuration(data.durationMs) : "—"}
                </span>
                <span className="flex items-center gap-1.5">
                  <FileText className="h-3.5 w-3.5" />
                  {data.manuscriptsIngested.toLocaleString()}
                </span>
                <span className="flex items-center gap-1.5">
                  <Link2 className="h-3.5 w-3.5" />
                  {data.versesLinked.toLocaleString()}
                </span>
              </div>

              {trigger.isError && (
                <p className="mt-3 text-sm text-red-600 dark:text-red-400">
                  {(trigger.error as Error).message}
                </p>
              )}
            </div>

            {data.status === "failed" && data.errorMessage && (
              <div className="rounded-xl border border-red-300 bg-red-50 p-4 dark:border-red-800 dark:bg-red-950">
                <p className="text-sm text-red-600 dark:text-red-400 font-mono">
                  {data.errorMessage}
                </p>
              </div>
            )}

            {/* Tabs */}
            <div className="rounded-xl border border-border bg-card">
              <div className="flex border-b border-border">
                {TABS.map((tab) => (
                  <button
                    key={tab.key}
                    onClick={() => setActiveTab(tab.key)}
                    className={`flex flex-1 items-center justify-center gap-2 px-4 py-3 text-sm font-medium transition-colors ${
                      activeTab === tab.key
                        ? "border-b-2 border-primary text-foreground"
                        : "text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    {tab.icon}
                    <span className="hidden sm:inline">{t(`tabs.${tab.key}`)}</span>
                  </button>
                ))}
              </div>
              <div className="p-4">
                {activeTab === "manuscripts" && <ManuscriptIngestionPanel />}
                {activeTab === "patristic" && <PatristicIngestionPanel />}
                {activeTab === "councils" && <CouncilIngestionPanel />}
                {activeTab === "bible" && <BibleIngestionPanel />}
                {activeTab === "llmQueue" && <LlmQueuePanel />}
              </div>
            </div>

          </>
        )}
      </div>
    </div>
  );
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remainingSec = seconds % 60;
  return `${minutes}m ${remainingSec}s`;
}
