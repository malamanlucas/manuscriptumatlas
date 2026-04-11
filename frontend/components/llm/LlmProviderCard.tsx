"use client";

import { useTranslations } from "next-intl";
import { CheckCircle, XCircle, Zap, DollarSign } from "lucide-react";
import type { LlmProviderSummaryDTO } from "@/types";

function formatTokens(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}

const ERROR_COLORS: Record<string, string> = {
  TIMEOUT: "bg-amber-500/10 text-amber-600 border-amber-500/30",
  RATE_LIMIT: "bg-orange-500/10 text-orange-600 border-orange-500/30",
  BAD_REQUEST: "bg-red-500/10 text-red-600 border-red-500/30",
  AUTH_ERROR: "bg-purple-500/10 text-purple-600 border-purple-500/30",
  SERVER_ERROR: "bg-red-500/10 text-red-600 border-red-500/30",
  OTHER: "bg-muted text-muted-foreground border-border",
};

export function LlmProviderCard({ summary, onErrorTypeClick }: { summary: LlmProviderSummaryDTO; onErrorTypeClick?: (type: string) => void }) {
  const t = useTranslations("llmUsage.summary");

  const successPct = summary.successRate;
  const barColor =
    successPct >= 95 ? "bg-emerald-500" : successPct >= 80 ? "bg-amber-500" : "bg-red-500";

  return (
    <div className="rounded-xl border border-border bg-card p-4 md:p-5">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-base font-semibold">{summary.provider}</h3>
        <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium">
          {summary.totalRequests} {t("totalRequests").toLowerCase()}
        </span>
      </div>

      {/* Success rate bar */}
      <div className="mb-3">
        <div className="mb-1 flex items-center justify-between text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <CheckCircle className="h-3 w-3 text-emerald-500" />
            {summary.successfulRequests}
          </span>
          <span className="flex items-center gap-1">
            <XCircle className="h-3 w-3 text-red-500" />
            {summary.failedRequests}
          </span>
        </div>
        <div className="h-2 w-full rounded-full bg-muted">
          <div
            className={`h-2 rounded-full ${barColor} transition-all`}
            style={{ width: `${Math.min(successPct, 100)}%` }}
          />
        </div>
        <p className="mt-1 text-right text-xs text-muted-foreground">
          {t("successRate")}: {successPct.toFixed(1)}%
        </p>
      </div>

      {/* Error breakdown per provider */}
      {summary.errorBreakdown && summary.errorBreakdown.length > 0 && (
        <div className="mb-3 flex flex-wrap gap-1.5">
          {summary.errorBreakdown.map((e) => (
            <button
              key={e.errorType}
              onClick={() => onErrorTypeClick?.(e.errorType)}
              className={`rounded border px-2 py-0.5 text-[10px] font-medium cursor-pointer transition-all hover:ring-1 hover:ring-current/30 ${ERROR_COLORS[e.errorType] ?? ERROR_COLORS.OTHER}`}
            >
              {e.count} {e.errorType}
            </button>
          ))}
        </div>
      )}

      {/* Tokens */}
      <div className="mb-3 grid grid-cols-2 gap-3">
        <div className="rounded-lg bg-muted/50 p-2.5">
          <p className="text-xs text-muted-foreground">{t("inputTokens")}</p>
          <p className="text-sm font-semibold">{formatTokens(summary.totalInputTokens)}</p>
        </div>
        <div className="rounded-lg bg-muted/50 p-2.5">
          <p className="text-xs text-muted-foreground">{t("outputTokens")}</p>
          <p className="text-sm font-semibold">{formatTokens(summary.totalOutputTokens)}</p>
        </div>
      </div>

      {/* Total tokens & cost */}
      <div className="flex items-center justify-between border-t border-border pt-3">
        <div className="flex items-center gap-1.5 text-sm">
          <Zap className="h-4 w-4 text-blue-500" />
          <span className="font-medium">{formatTokens(summary.totalTokens)}</span>
          <span className="text-xs text-muted-foreground">{t("totalTokens").toLowerCase()}</span>
        </div>
        <div className="flex items-center gap-1 text-sm font-semibold text-emerald-600 dark:text-emerald-400">
          <DollarSign className="h-4 w-4" />
          {summary.estimatedCostUsd.toFixed(4)}
        </div>
      </div>
    </div>
  );
}
