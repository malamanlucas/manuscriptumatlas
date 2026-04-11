"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { X, Loader2 } from "lucide-react";
import { useLlmErrorsByType } from "@/hooks/useLlmUsage";
import type { LlmUsageLogDTO } from "@/types";

const ERROR_COLORS: Record<string, string> = {
  TIMEOUT: "bg-amber-500/10 text-amber-600 border-amber-500/30",
  RATE_LIMIT: "bg-orange-500/10 text-orange-600 border-orange-500/30",
  BAD_REQUEST: "bg-red-500/10 text-red-600 border-red-500/30",
  AUTH_ERROR: "bg-purple-500/10 text-purple-600 border-purple-500/30",
  SERVER_ERROR: "bg-red-500/10 text-red-600 border-red-500/30",
  OTHER: "bg-muted text-muted-foreground border-border",
};

function formatTime(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  } catch {
    return iso;
  }
}

function formatMs(ms: number): string {
  if (ms >= 1000) return `${(ms / 1000).toFixed(1)}s`;
  return `${ms}ms`;
}

interface ErrorDetailModalProps {
  errorType?: string;
  singleLog?: LlmUsageLogDTO;
  period: string;
  onClose: () => void;
}

export function ErrorDetailModal({ errorType, singleLog, period, onClose }: ErrorDetailModalProps) {
  const t = useTranslations("llmUsage.errorModal");
  const { data: errors, isLoading } = useLlmErrorsByType(errorType ?? null, period);

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  const logs = singleLog ? [singleLog] : errors;
  const title = singleLog
    ? t("title")
    : errorType
      ? t("titleByType", { type: errorType })
      : t("title");

  const colorClass = errorType ? ERROR_COLORS[errorType] ?? ERROR_COLORS.OTHER : "";

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      {/* Modal */}
      <div className="relative z-50 w-full max-w-2xl max-h-[80vh] flex flex-col rounded-xl border border-border bg-card shadow-xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-semibold">{title}</h2>
            {errorType && (
              <span className={`rounded-md border px-2 py-0.5 text-xs font-medium ${colorClass}`}>
                {errorType}
              </span>
            )}
          </div>
          <button
            onClick={onClose}
            className="rounded-md p-1 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-4">
          {isLoading && !singleLog && (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          )}

          {!isLoading && (!logs || logs.length === 0) && (
            <p className="text-center text-sm text-muted-foreground py-8">{t("noErrors")}</p>
          )}

          {logs && logs.length > 0 && (
            <div className="space-y-3">
              {!singleLog && (
                <p className="text-xs text-muted-foreground">
                  {t("recentCount", { count: logs.length })}
                </p>
              )}

              {logs.map((log) => (
                <div
                  key={log.id}
                  className="rounded-lg border border-border bg-muted/30 p-3 space-y-2"
                >
                  {/* Row 1: provider + model + timestamp */}
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="text-xs font-semibold">{log.provider}</span>
                      <span className="text-xs text-muted-foreground truncate">{log.model}</span>
                    </div>
                    <span className="text-[10px] text-muted-foreground shrink-0">
                      {formatTime(log.createdAt)}
                    </span>
                  </div>

                  {/* Row 2: label + latency */}
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs text-muted-foreground truncate" title={log.label}>
                      {log.label || "-"}
                    </span>
                    <span className="text-[10px] tabular-nums text-muted-foreground shrink-0">
                      {formatMs(log.latencyMs)}
                    </span>
                  </div>

                  {/* Row 3: error message */}
                  {log.errorMessage && (
                    <pre className="text-[11px] leading-relaxed text-red-600 dark:text-red-400 bg-red-500/5 rounded-md p-2 whitespace-pre-wrap break-all overflow-x-auto max-h-40">
                      {log.errorMessage}
                    </pre>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
