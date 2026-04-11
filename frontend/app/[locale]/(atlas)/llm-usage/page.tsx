"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { BrainCircuit, Loader2, RefreshCw } from "lucide-react";
import { AuthGate } from "@/components/observatory/AuthGate";
import { LlmProviderCard } from "@/components/llm/LlmProviderCard";
import { LlmRateLimitPanel } from "@/components/llm/LlmRateLimitPanel";
import { LlmUsageTable } from "@/components/llm/LlmUsageTable";
import { useLlmDashboard } from "@/hooks/useLlmUsage";
import { useQueryClient } from "@tanstack/react-query";
import { ErrorDetailModal } from "@/components/llm/ErrorDetailModal";
import type { LlmProviderSummaryDTO, LlmErrorSummary, LlmUsageLogDTO } from "@/types";

const PERIODS = ["5m", "15m", "30m", "1h", "3h", "6h", "12h", "today", "7d", "30d", "all"] as const;

function formatTokens(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}

function formatMs(ms: number): string {
  if (ms >= 1000) return `${(ms / 1000).toFixed(1)}s`;
  return `${ms}ms`;
}

function ModelBreakdownTable({ models }: { models: LlmProviderSummaryDTO[] }) {
  const t = useTranslations("llmUsage");

  if (!models || models.length === 0) return null;

  return (
    <div className="rounded-xl border border-border bg-card">
      <div className="border-b border-border px-4 py-3">
        <h3 className="text-sm font-semibold">{t("modelBreakdown")}</h3>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-border text-xs text-muted-foreground">
              <th className="px-3 py-2.5 font-medium">{t("table.model")}</th>
              <th className="px-3 py-2.5 font-medium text-right">{t("table.totalReqs")}</th>
              <th className="px-3 py-2.5 font-medium text-right">{t("table.successRate")}</th>
              <th className="hidden px-3 py-2.5 font-medium text-right sm:table-cell">{t("table.avgLatency")}</th>
              <th className="hidden px-3 py-2.5 font-medium text-right md:table-cell">{t("table.tokens")}</th>
              <th className="hidden px-3 py-2.5 font-medium text-right lg:table-cell">{t("table.cost")}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {models.map((m, idx) => {
              const rateColor = m.successRate >= 95 ? "text-emerald-500" : m.successRate >= 70 ? "text-amber-500" : "text-red-500";
              return (
                <tr key={idx} className="hover:bg-muted/30 transition-colors">
                  <td className="px-3 py-2">
                    <div>
                      <span className="font-medium text-sm">{m.model ?? "unknown"}</span>
                      <span className="ml-2 text-xs text-muted-foreground">{m.provider}</span>
                    </div>
                  </td>
                  <td className="px-3 py-2 text-right tabular-nums font-medium">{m.totalRequests}</td>
                  <td className={`px-3 py-2 text-right tabular-nums font-semibold ${rateColor}`}>
                    {m.successRate.toFixed(1)}%
                    <span className="ml-1 text-xs text-muted-foreground font-normal">
                      ({m.failedRequests} err)
                    </span>
                  </td>
                  <td className="hidden px-3 py-2 text-right tabular-nums sm:table-cell">
                    {m.avgLatencyMs ? formatMs(m.avgLatencyMs) : "—"}
                  </td>
                  <td className="hidden px-3 py-2 text-right tabular-nums md:table-cell">
                    {formatTokens(m.totalTokens)}
                  </td>
                  <td className="hidden px-3 py-2 text-right tabular-nums lg:table-cell">
                    ${m.estimatedCostUsd.toFixed(4)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ErrorSummaryPanel({ errors, onErrorTypeClick }: { errors: LlmErrorSummary[]; onErrorTypeClick: (type: string) => void }) {
  const t = useTranslations("llmUsage");

  if (!errors || errors.length === 0) return null;

  const errorColors: Record<string, string> = {
    TIMEOUT: "bg-amber-500/10 text-amber-600 border-amber-500/30",
    RATE_LIMIT: "bg-orange-500/10 text-orange-600 border-orange-500/30",
    BAD_REQUEST: "bg-red-500/10 text-red-600 border-red-500/30",
    AUTH_ERROR: "bg-purple-500/10 text-purple-600 border-purple-500/30",
    SERVER_ERROR: "bg-red-500/10 text-red-600 border-red-500/30",
    OTHER: "bg-muted text-muted-foreground border-border",
  };

  const total = errors.reduce((sum, e) => sum + e.count, 0);

  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <h3 className="text-sm font-semibold mb-3">{t("errorBreakdown")}</h3>
      <div className="flex flex-wrap gap-2">
        {errors.map((e) => (
          <button
            key={e.errorType}
            onClick={() => onErrorTypeClick(e.errorType)}
            className={`rounded-lg border px-3 py-2 cursor-pointer transition-all hover:ring-2 hover:ring-offset-1 hover:ring-current/30 ${errorColors[e.errorType] ?? errorColors.OTHER}`}
          >
            <span className="text-sm font-bold">{e.count}</span>
            <span className="ml-1.5 text-xs">{e.errorType}</span>
            <span className="ml-1 text-xs opacity-60">
              ({((e.count / total) * 100).toFixed(0)}%)
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}

function LlmUsageContent() {
  const t = useTranslations("llmUsage");
  const [period, setPeriod] = useState<string>("7d");
  const [selectedErrorType, setSelectedErrorType] = useState<string | null>(null);
  const [selectedErrorLog, setSelectedErrorLog] = useState<LlmUsageLogDTO | null>(null);
  const { data, isLoading, isError, dataUpdatedAt } = useLlmDashboard(period);
  const queryClient = useQueryClient();

  const lastUpdate = dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString() : null;

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <BrainCircuit className="h-6 w-6 text-purple-500" />
          <div>
            <h1 className="text-lg font-bold md:text-xl">{t("title")}</h1>
            <p className="text-xs text-muted-foreground md:text-sm">{t("subtitle")}</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Last update indicator */}
          {lastUpdate && (
            <span className="text-[10px] text-muted-foreground/60">
              {t("lastUpdate")}: {lastUpdate}
            </span>
          )}

          {/* Manual refresh */}
          <button
            onClick={() => queryClient.invalidateQueries({ queryKey: ["llm-usage"] })}
            className="rounded-md p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
            title={t("refresh")}
          >
            <RefreshCw className="h-4 w-4" />
          </button>

          {/* Period selector */}
          <div className="flex gap-1 rounded-lg bg-muted p-1">
            {PERIODS.map((p) => (
              <button
                key={p}
                onClick={() => setPeriod(p)}
                className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                  period === p
                    ? "bg-background text-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {t(`period.${p}`)}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {/* Error */}
      {isError && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
          {t("error")}
        </div>
      )}

      {data && (
        <>
          {/* Provider summary cards */}
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            {data.providerSummaries.map((s) => (
              <LlmProviderCard key={s.provider} summary={s} onErrorTypeClick={setSelectedErrorType} />
            ))}
          </div>

          {/* Error breakdown */}
          {data.errorSummary && data.errorSummary.length > 0 && (
            <ErrorSummaryPanel errors={data.errorSummary} onErrorTypeClick={setSelectedErrorType} />
          )}

          {/* Model breakdown table */}
          {data.modelSummaries && data.modelSummaries.length > 0 && (
            <ModelBreakdownTable models={data.modelSummaries} />
          )}

          {/* Rate limiter status */}
          {data.rateLimiterStatus.length > 0 && (
            <LlmRateLimitPanel statuses={data.rateLimiterStatus} />
          )}

          {/* Recent logs table */}
          <div>
            <h2 className="mb-3 text-sm font-semibold">{t("table.title")}</h2>
            <LlmUsageTable logs={data.recentLogs} onErrorClick={setSelectedErrorLog} />
          </div>
        </>
      )}

      {/* Error detail modal */}
      {(selectedErrorType || selectedErrorLog) && (
        <ErrorDetailModal
          errorType={selectedErrorType ?? undefined}
          singleLog={selectedErrorLog ?? undefined}
          period={period}
          onClose={() => { setSelectedErrorType(null); setSelectedErrorLog(null); }}
        />
      )}
    </div>
  );
}

export default function LlmUsagePage() {
  return (
    <AuthGate requiredRole="ADMIN">
      <LlmUsageContent />
    </AuthGate>
  );
}
