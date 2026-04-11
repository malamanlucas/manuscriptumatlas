"use client";

import { useTranslations } from "next-intl";
import { useLlmQueueStats, useRetryFailedQueue, useClearQueuePhase, useUnstickProcessing } from "@/hooks/useLlmQueue";
import {
  Loader2,
  RotateCcw,
  Trash2,
  Clock,
  Cpu,
  CheckCircle2,
  CircleCheckBig,
  AlertCircle,
  Zap,
  PlayCircle,
} from "lucide-react";
import type { QueuePhaseStatsDTO } from "@/types";

export function LlmQueuePanel() {
  const t = useTranslations("ingestion.llmQueue");
  const { data, isLoading, error } = useLlmQueueStats();
  const retryAll = useRetryFailedQueue();
  const clearPhase = useClearQueuePhase();
  const unstick = useUnstickProcessing();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-300 bg-red-50 p-4 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
        {(error as Error).message}
      </div>
    );
  }

  if (!data) return null;

  const total = data.totalPending + data.totalProcessing + data.totalCompleted + data.totalApplied + data.totalFailed;

  if (total === 0) {
    return (
      <div className="py-12 text-center">
        <Cpu className="mx-auto h-10 w-10 text-muted-foreground/40" />
        <p className="mt-3 text-sm font-medium text-muted-foreground">{t("noItems")}</p>
        <p className="mt-1 text-xs text-muted-foreground/70">{t("emptyQueue")}</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Banner: waiting for /run-llm */}
      {data.totalPending > 0 && (
        <div className="flex items-center gap-2 rounded-lg border border-amber-300 bg-amber-50 px-4 py-2.5 dark:border-amber-700 dark:bg-amber-950">
          <Zap className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
          <span className="text-sm text-amber-700 dark:text-amber-300">
            {t("waitingForRunLlm", { count: data.totalPending })}
          </span>
        </div>
      )}

      {/* Stat cards */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
        <StatCard label={t("pending")} value={data.totalPending} color="blue" icon={<Clock className="h-4 w-4" />} />
        <StatCard label={t("processing")} value={data.totalProcessing} color="amber" icon={<Loader2 className="h-4 w-4 animate-spin" />} />
        <StatCard label={t("completed")} value={data.totalCompleted} color="emerald" icon={<CheckCircle2 className="h-4 w-4" />} />
        <StatCard label={t("applied")} value={data.totalApplied} color="teal" icon={<CircleCheckBig className="h-4 w-4" />} />
        <StatCard label={t("failed")} value={data.totalFailed} color="red" icon={<AlertCircle className="h-4 w-4" />} />
      </div>

      {/* Action buttons */}
      {(data.totalFailed > 0 || data.totalProcessing > 0) && (
        <div className="flex justify-end gap-2">
          {data.totalProcessing > 0 && (
            <button
              onClick={() => {
                if (window.confirm(t("confirmUnstick"))) unstick.mutate(undefined);
              }}
              disabled={unstick.isPending}
              className="flex items-center gap-2 rounded-lg border border-amber-500/50 px-3 py-1.5 text-xs font-medium text-amber-500 transition-colors hover:bg-amber-500/10 disabled:opacity-50"
            >
              {unstick.isPending ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
              ) : (
                <PlayCircle className="h-3.5 w-3.5" />
              )}
              {t("unstickAll")}
            </button>
          )}
          {data.totalFailed > 0 && (
            <button
              onClick={() => {
                if (window.confirm(t("confirmRetryAll"))) retryAll.mutate(undefined);
              }}
              disabled={retryAll.isPending}
              className="flex items-center gap-2 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:bg-accent disabled:opacity-50"
            >
              {retryAll.isPending ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
              ) : (
                <RotateCcw className="h-3.5 w-3.5" />
              )}
              {t("retryAll")}
            </button>
          )}
        </div>
      )}

      {/* Per-phase table */}
      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-muted/50">
              <th className="px-3 py-2 text-left font-medium text-muted-foreground">{t("phase")}</th>
              <th className="px-3 py-2 text-right font-medium text-blue-600 dark:text-blue-400">{t("pending")}</th>
              <th className="px-3 py-2 text-right font-medium text-amber-600 dark:text-amber-400">{t("processing")}</th>
              <th className="px-3 py-2 text-right font-medium text-emerald-600 dark:text-emerald-400">{t("completed")}</th>
              <th className="px-3 py-2 text-right font-medium text-teal-600 dark:text-teal-400">{t("applied")}</th>
              <th className="px-3 py-2 text-right font-medium text-red-600 dark:text-red-400">{t("failed")}</th>
              <th className="px-3 py-2 text-right font-medium text-muted-foreground">{t("actions")}</th>
            </tr>
          </thead>
          <tbody>
            {data.byPhase.map((phase) => (
              <PhaseRow
                key={phase.phaseName}
                phase={phase}
                onRetry={() => {
                  if (window.confirm(t("confirmRetryPhase", { phase: phase.phaseName })))
                    retryAll.mutate(phase.phaseName);
                }}
                onClear={() => {
                  if (window.confirm(t("confirmClearPhase", { phase: phase.phaseName })))
                    clearPhase.mutate(phase.phaseName);
                }}
                isRetrying={retryAll.isPending}
                isClearing={clearPhase.isPending}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatCard({
  label,
  value,
  color,
  icon,
}: {
  label: string;
  value: number;
  color: "blue" | "amber" | "emerald" | "teal" | "red";
  icon: React.ReactNode;
}) {
  const colorMap = {
    blue: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
    amber: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
    emerald: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
    teal: "bg-teal-500/10 text-teal-600 dark:text-teal-400",
    red: "bg-red-500/10 text-red-600 dark:text-red-400",
  };

  return (
    <div className={`rounded-lg p-3 ${colorMap[color]}`}>
      <div className="flex items-center gap-2">
        {icon}
        <span className="text-xs font-medium opacity-80">{label}</span>
      </div>
      <p className="mt-1 text-2xl font-bold">{value.toLocaleString()}</p>
    </div>
  );
}

function PhaseRow({
  phase,
  onRetry,
  onClear,
  isRetrying,
  isClearing,
}: {
  phase: QueuePhaseStatsDTO;
  onRetry: () => void;
  onClear: () => void;
  isRetrying: boolean;
  isClearing: boolean;
}) {
  return (
    <tr className="border-b border-border last:border-0 hover:bg-muted/30">
      <td className="px-3 py-2 font-mono text-xs">{phase.phaseName}</td>
      <td className="px-3 py-2 text-right tabular-nums">
        {phase.pending > 0 ? (
          <span className="font-medium text-blue-600 dark:text-blue-400">{phase.pending}</span>
        ) : (
          <span className="text-muted-foreground">0</span>
        )}
      </td>
      <td className="px-3 py-2 text-right tabular-nums">
        {phase.processing > 0 ? (
          <span className="font-medium text-amber-600 dark:text-amber-400">{phase.processing}</span>
        ) : (
          <span className="text-muted-foreground">0</span>
        )}
      </td>
      <td className="px-3 py-2 text-right tabular-nums">
        {phase.completed > 0 ? (
          <span className="font-medium text-emerald-600 dark:text-emerald-400">{phase.completed}</span>
        ) : (
          <span className="text-muted-foreground">0</span>
        )}
      </td>
      <td className="px-3 py-2 text-right tabular-nums">
        {phase.applied > 0 ? (
          <span className="font-medium text-teal-600 dark:text-teal-400">{phase.applied}</span>
        ) : (
          <span className="text-muted-foreground">0</span>
        )}
      </td>
      <td className="px-3 py-2 text-right tabular-nums">
        {phase.failed > 0 ? (
          <span className="font-medium text-red-600 dark:text-red-400">{phase.failed}</span>
        ) : (
          <span className="text-muted-foreground">0</span>
        )}
      </td>
      <td className="px-3 py-2 text-right">
        <div className="flex items-center justify-end gap-1">
          {phase.failed > 0 && (
            <button
              onClick={onRetry}
              disabled={isRetrying}
              className="rounded p-1 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground disabled:opacity-50"
              title="Retry failed"
            >
              <RotateCcw className="h-3.5 w-3.5" />
            </button>
          )}
          <button
            onClick={onClear}
            disabled={isClearing}
            className="rounded p-1 text-muted-foreground transition-colors hover:bg-red-500/10 hover:text-red-500 disabled:opacity-50"
            title="Clear phase"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </div>
      </td>
    </tr>
  );
}
