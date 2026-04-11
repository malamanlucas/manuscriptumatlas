"use client";

import { useMemo, useState, useEffect } from "react";
import { useTranslations } from "next-intl";
import type { PhaseStatusDTO } from "@/types";
import { Loader2, Play, AlertCircle, CheckCircle2, Clock3, Trash2, Check } from "lucide-react";

interface IngestionPhasePanelProps {
  phasesOrder: string[];
  phasesData: PhaseStatusDTO[] | undefined;
  phaseLabels: Record<string, string>;
  phaseDescriptions?: Record<string, string>;
  onRunPhase: (phase: string) => void;
  onRunAll: () => void;
  confirmRunAllMessage: string;
  isRunPhasePending: boolean;
  runPhaseVariable?: string;
  isRunAllPending: boolean;
  onReset?: () => void;
  confirmResetMessage?: string;
  isResetPending?: boolean;
  resetSuccess?: boolean;
  footer?: React.ReactNode;
}

export function IngestionPhasePanel({
  phasesOrder,
  phasesData,
  phaseLabels,
  phaseDescriptions,
  onRunPhase,
  onRunAll,
  confirmRunAllMessage,
  isRunPhasePending,
  runPhaseVariable,
  isRunAllPending,
  onReset,
  confirmResetMessage,
  isResetPending,
  resetSuccess,
  footer,
}: IngestionPhasePanelProps) {
  const t = useTranslations("ingestion.phasePanel");
  const [showResetOk, setShowResetOk] = useState(false);

  useEffect(() => {
    if (resetSuccess) {
      setShowResetOk(true);
      const timer = setTimeout(() => setShowResetOk(false), 2000);
      return () => clearTimeout(timer);
    }
  }, [resetSuccess]);

  const phases = useMemo(() => {
    const byName = new Map((phasesData ?? []).map((p) => [p.phaseName, p]));
    return phasesOrder.map((phaseName) => {
      return (
        byName.get(phaseName) ?? {
          phaseName,
          status: "idle",
          itemsProcessed: 0,
          itemsTotal: 0,
        }
      );
    });
  }, [phasesData, phasesOrder]);

  const isAnyRunning = phases.some((p) => p.status === "running");
  const completedCount = phases.filter((p) => p.status === "success").length;
  const failedCount = phases.filter((p) => p.status === "failed").length;

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3 text-xs text-muted-foreground">
          {completedCount > 0 && (
            <span className="inline-flex items-center gap-1 text-emerald-500">
              <CheckCircle2 className="h-3.5 w-3.5" />
              {completedCount}/{phasesOrder.length}
            </span>
          )}
          {failedCount > 0 && (
            <span className="inline-flex items-center gap-1 text-red-500">
              <AlertCircle className="h-3.5 w-3.5" />
              {failedCount} {t("failed")}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {onReset && (
            <button
              onClick={() => {
                if (window.confirm(confirmResetMessage ?? t("confirmReset"))) onReset();
              }}
              disabled={isAnyRunning || isResetPending}
              className="rounded-lg border border-red-500/50 px-3 py-1.5 text-xs font-medium text-red-400 hover:bg-red-500/10 disabled:opacity-50"
            >
              {isResetPending ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : showResetOk ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Trash2 className="h-3.5 w-3.5" />}
            </button>
          )}
          <button
            onClick={() => {
              if (window.confirm(confirmRunAllMessage)) onRunAll();
            }}
            disabled={isAnyRunning || isRunAllPending}
            className="rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground disabled:opacity-50"
          >
            {isRunAllPending ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : t("runAll")}
          </button>
        </div>
      </div>

      <div className="space-y-1.5">
        {phases.map((phase) => (
          <PhaseRow
            key={phase.phaseName}
            phase={phase}
            label={phaseLabels[phase.phaseName] ?? phase.phaseName}
            description={phaseDescriptions?.[phase.phaseName]}
            onRun={() => onRunPhase(phase.phaseName)}
            runningMutation={isRunPhasePending && runPhaseVariable === phase.phaseName}
            disabled={isAnyRunning}
          />
        ))}
      </div>

      {footer && <div className="pt-2 text-xs text-muted-foreground">{footer}</div>}
    </div>
  );
}

function PhaseRow({
  phase,
  label,
  description,
  onRun,
  runningMutation,
  disabled,
}: {
  phase: PhaseStatusDTO;
  label: string;
  description?: string;
  onRun: () => void;
  runningMutation: boolean;
  disabled: boolean;
}) {
  const t = useTranslations("ingestion.phasePanel");
  const progress =
    phase.itemsTotal > 0 ? Math.min(100, Math.round((phase.itemsProcessed / phase.itemsTotal) * 100)) : 0;
  const statusUi = getStatusUi(phase.status);

  return (
    <div className="group flex items-center gap-3 rounded-lg border border-border px-3 py-2.5">
      <div className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full ${statusUi.bg}`}>
        {statusUi.icon}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline gap-2">
          <p className="truncate text-sm font-medium">{label}</p>
          <span className="shrink-0 text-xs text-muted-foreground">
            {phase.itemsTotal > 0 ? `${phase.itemsProcessed}/${phase.itemsTotal}` : ""}
          </span>
        </div>
        {description && (
          <p className="text-xs text-muted-foreground/70">{description}</p>
        )}
        {phase.status === "running" && (
          <div className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-muted">
            <div className="h-full bg-blue-500 transition-all duration-500" style={{ width: `${progress}%` }} />
          </div>
        )}
        {phase.status === "failed" && phase.errorMessage && (
          <details className="mt-1 text-xs text-red-500">
            <summary className="cursor-pointer">{t("errorDetails")}</summary>
            <p className="mt-1 whitespace-pre-wrap">{phase.errorMessage}</p>
          </details>
        )}
      </div>
      {phase.status !== "running" && (
        <button
          onClick={onRun}
          disabled={disabled || runningMutation}
          className="rounded-md p-1.5 text-muted-foreground opacity-0 transition-opacity hover:bg-accent group-hover:opacity-100 disabled:opacity-50"
          title={t("runPhase")}
        >
          {runningMutation ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Play className="h-3.5 w-3.5" />}
        </button>
      )}
    </div>
  );
}

function getStatusUi(status: string) {
  switch (status) {
    case "running":
      return {
        bg: "bg-blue-500/15",
        icon: <Loader2 className="h-3.5 w-3.5 animate-spin text-blue-500" />,
      };
    case "success":
      return {
        bg: "bg-emerald-500/15",
        icon: <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />,
      };
    case "failed":
      return {
        bg: "bg-red-500/15",
        icon: <AlertCircle className="h-3.5 w-3.5 text-red-500" />,
      };
    default:
      return {
        bg: "bg-muted",
        icon: <Clock3 className="h-3.5 w-3.5 text-muted-foreground" />,
      };
  }
}
