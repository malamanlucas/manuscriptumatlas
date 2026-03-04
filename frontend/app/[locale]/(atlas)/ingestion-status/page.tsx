"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { AuthGate } from "@/components/observatory/AuthGate";
import {
  useIngestionStatus,
  useTriggerIngestion,
  useResetAndReIngest,
  useDatingEnrichment,
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
  Trash2,
  Sparkles,
} from "lucide-react";
import { CouncilIngestionPanel } from "@/components/ingestion/CouncilIngestionPanel";

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
  const reset = useResetAndReIngest();
  const enrich = useDatingEnrichment();
  const [showConfirm, setShowConfirm] = useState(false);
  const [enrichDomain, setEnrichDomain] = useState<"fathers" | "manuscripts" | "all">("fathers");

  const STATUS_CONFIG: Record<
    string,
    { label: string; color: string; bg: string; icon: React.ReactNode }
  > = {
    idle: {
      label: t("idle"),
      color: "text-gray-600 dark:text-gray-400",
      bg: "bg-gray-100 dark:bg-gray-800",
      icon: <Clock className="h-5 w-5" />,
    },
    running: {
      label: t("running"),
      color: "text-blue-600 dark:text-blue-400",
      bg: "bg-blue-100 dark:bg-blue-900",
      icon: <Loader2 className="h-5 w-5 animate-spin" />,
    },
    success: {
      label: t("success"),
      color: "text-emerald-600 dark:text-emerald-400",
      bg: "bg-emerald-100 dark:bg-emerald-900",
      icon: <CheckCircle className="h-5 w-5" />,
    },
    failed: {
      label: t("failed"),
      color: "text-red-600 dark:text-red-400",
      bg: "bg-red-100 dark:bg-red-900",
      icon: <XCircle className="h-5 w-5" />,
    },
  };

  const statusKey = data?.isRunning ? "running" : (data?.status ?? "idle");
  const config = STATUS_CONFIG[statusKey] ?? STATUS_CONFIG.idle;

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="p-4 md:p-6 space-y-6 max-w-3xl">
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
            <div className="rounded-xl border border-border bg-card p-6">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-3">
                  <div className={`rounded-lg p-2 ${config.bg} ${config.color}`}>
                    {config.icon}
                  </div>
                  <div>
                    <p className={`text-lg font-semibold ${config.color}`}>
                      {config.label}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {data.enableIngestion
                        ? t("enabled")
                        : t("disabled")}
                    </p>
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
              {(trigger.isError || reset.isError) && (
                <p className="mt-3 text-sm text-red-600 dark:text-red-400">
                  {((trigger.error || reset.error) as Error).message}
                </p>
              )}
            </div>

            {/* Reset + Re-ingest */}
            <div className="rounded-xl border border-red-300/30 bg-red-950/20 p-5">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm font-semibold text-red-400">
                    {t("resetTitle")}
                  </p>
                  <p className="text-xs text-red-400/70 mt-0.5">
                    {t("resetDescription")}
                  </p>
                </div>
                {!showConfirm ? (
                  <button
                    onClick={() => setShowConfirm(true)}
                    disabled={data.isRunning || reset.isPending || !data.enableIngestion}
                    className="flex shrink-0 items-center gap-2 rounded-lg border border-red-500/50 bg-red-500/10 px-4 py-2 text-sm font-medium text-red-400 transition-colors hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <Trash2 className="h-4 w-4" />
                    {t("resetButton")}
                  </button>
                ) : (
                  <div className="flex shrink-0 items-center gap-2">
                    <button
                      onClick={() => setShowConfirm(false)}
                      className="rounded-lg border border-border px-3 py-2 text-sm text-muted-foreground hover:bg-accent"
                    >
                      {t("cancel")}
                    </button>
                    <button
                      onClick={() => {
                        reset.mutate();
                        setShowConfirm(false);
                      }}
                      disabled={reset.isPending}
                      className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700"
                    >
                      {reset.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Trash2 className="h-4 w-4" />
                      )}
                      {t("confirmReset")}
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Dating Enrichment */}
            <div className="rounded-xl border border-amber-500/30 bg-amber-950/10 p-5">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-amber-100 p-2 dark:bg-amber-900">
                    <Sparkles className="h-5 w-5 text-amber-500" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-foreground">
                      {t("enrichTitle")}
                    </p>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {t("enrichDescription")}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <select
                    value={enrichDomain}
                    onChange={(e) => setEnrichDomain(e.target.value as "fathers" | "manuscripts" | "all")}
                    className="rounded-lg border border-border bg-background px-3 py-2 text-sm"
                  >
                    <option value="fathers">{t("enrichFathers")}</option>
                    <option value="manuscripts">{t("enrichManuscripts")}</option>
                    <option value="all">{t("enrichAll")}</option>
                  </select>
                  <button
                    onClick={() => enrich.mutate({ domain: enrichDomain, limit: 50 })}
                    disabled={enrich.isPending}
                    className="flex shrink-0 items-center gap-2 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-amber-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {enrich.isPending ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Sparkles className="h-4 w-4" />
                    )}
                    {t("enrichButton")}
                  </button>
                </div>
              </div>
              {enrich.isSuccess && (
                <p className="mt-3 text-sm text-emerald-500">
                  {(enrich.data as { message: string }).message}
                </p>
              )}
              {enrich.isError && (
                <p className="mt-3 text-sm text-red-400">
                  {(enrich.error as Error).message}
                </p>
              )}
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <MetricCard
                icon={<Timer className="h-5 w-5 text-blue-500" />}
                bg="bg-blue-100 dark:bg-blue-900"
                label={t("duration")}
                value={
                  data.durationMs != null
                    ? formatDuration(data.durationMs)
                    : "—"
                }
              />
              <MetricCard
                icon={<FileText className="h-5 w-5 text-amber-500" />}
                bg="bg-amber-100 dark:bg-amber-900"
                label={t("manuscripts")}
                value={data.manuscriptsIngested.toLocaleString()}
              />
              <MetricCard
                icon={<Link2 className="h-5 w-5 text-emerald-500" />}
                bg="bg-emerald-100 dark:bg-emerald-900"
                label={t("versesLinked")}
                value={data.versesLinked.toLocaleString()}
              />
              <MetricCard
                icon={<Clock className="h-5 w-5 text-violet-500" />}
                bg="bg-violet-100 dark:bg-violet-900"
                label={t("lastRun")}
                value={
                  data.startedAt
                    ? new Date(data.startedAt).toLocaleString()
                    : tc("never")
                }
              />
            </div>

            <CouncilIngestionPanel />

            {data.status === "failed" && data.errorMessage && (
              <div className="rounded-xl border border-red-300 bg-red-50 p-5 dark:border-red-800 dark:bg-red-950">
                <h3 className="text-sm font-semibold text-red-700 dark:text-red-300 mb-2">
                  {tc("error")}
                </h3>
                <p className="text-sm text-red-600 dark:text-red-400 font-mono">
                  {data.errorMessage}
                </p>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function MetricCard({
  icon,
  bg,
  label,
  value,
}: {
  icon: React.ReactNode;
  bg: string;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-3">
        <div className={`rounded-lg p-2 ${bg}`}>{icon}</div>
        <div>
          <p className="text-lg font-bold">{value}</p>
          <p className="text-xs text-muted-foreground">{label}</p>
        </div>
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
