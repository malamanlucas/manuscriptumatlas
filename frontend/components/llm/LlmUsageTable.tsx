"use client";

import { useTranslations } from "next-intl";
import { Check, X } from "lucide-react";
import type { LlmUsageLogDTO } from "@/types";

function formatMs(ms: number): string {
  if (ms >= 1000) return `${(ms / 1000).toFixed(1)}s`;
  return `${ms}ms`;
}

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

export function LlmUsageTable({ logs, onErrorClick }: { logs: LlmUsageLogDTO[]; onErrorClick?: (log: LlmUsageLogDTO) => void }) {
  const t = useTranslations("llmUsage.table");

  if (logs.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-8 text-center text-sm text-muted-foreground">
        {t("noData")}
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-border bg-card">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-border text-xs text-muted-foreground">
              <th className="px-3 py-2.5 font-medium">{t("status")}</th>
              <th className="px-3 py-2.5 font-medium">{t("provider")}</th>
              <th className="hidden px-3 py-2.5 font-medium sm:table-cell">{t("model")}</th>
              <th className="px-3 py-2.5 font-medium">{t("label")}</th>
              <th className="px-3 py-2.5 font-medium text-right">{t("tokens")}</th>
              <th className="hidden px-3 py-2.5 font-medium text-right md:table-cell">{t("cost")}</th>
              <th className="hidden px-3 py-2.5 font-medium text-right sm:table-cell">{t("latency")}</th>
              <th className="hidden px-3 py-2.5 font-medium text-right lg:table-cell">{t("time")}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {logs.map((log) => (
              <tr key={log.id} className="hover:bg-muted/30 transition-colors">
                <td className="px-3 py-2">
                  {log.success ? (
                    <Check className="h-4 w-4 text-emerald-500" />
                  ) : (
                    <button
                      onClick={() => onErrorClick?.(log)}
                      className="cursor-pointer rounded p-0.5 hover:bg-red-500/10 transition-colors"
                      title={log.errorMessage}
                    >
                      <X className="h-4 w-4 text-red-500" />
                    </button>
                  )}
                </td>
                <td className="px-3 py-2 font-medium">{log.provider}</td>
                <td className="hidden px-3 py-2 text-muted-foreground sm:table-cell">{log.model}</td>
                <td className="max-w-[200px] truncate px-3 py-2" title={log.label}>
                  {log.label || "—"}
                </td>
                <td className="px-3 py-2 text-right tabular-nums">
                  <span className="text-muted-foreground">{log.inputTokens}</span>
                  {" / "}
                  <span className="font-medium">{log.outputTokens}</span>
                </td>
                <td className="hidden px-3 py-2 text-right tabular-nums md:table-cell">
                  ${log.estimatedCostUsd.toFixed(4)}
                </td>
                <td className="hidden px-3 py-2 text-right tabular-nums sm:table-cell">
                  {formatMs(log.latencyMs)}
                </td>
                <td className="hidden px-3 py-2 text-right text-xs text-muted-foreground lg:table-cell">
                  {formatTime(log.createdAt)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
