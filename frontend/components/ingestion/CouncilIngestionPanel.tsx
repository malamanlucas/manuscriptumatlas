"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import {
  useCouncilIngestionPhases,
  useRunCouncilPhase,
  useRunCouncilPhases,
  useRunAllCouncilPhases,
  useCouncilIngestionCache,
} from "@/hooks/useCouncilIngestion";
import type { PhaseStatusDTO } from "@/types";
import { Loader2, Play, AlertCircle, CheckCircle2, Clock3 } from "lucide-react";

const PHASES_ORDER = [
  "council_seed",
  "council_extract_schaff",
  "council_extract_hefele",
  "council_extract_catholic_enc",
  "council_extract_fordham",
  "council_extract_wikidata",
  "council_extract_wikipedia",
  "council_consensus",
  "council_summaries",
  "council_overview_enrichment",
  "council_translate_all",
  "heresy_translate_all",
];

export function CouncilIngestionPanel() {
  const t = useTranslations("ingestion.councilIngestion");
  const phasesQuery = useCouncilIngestionPhases();
  const cacheQuery = useCouncilIngestionCache();
  const runOne = useRunCouncilPhase();
  const runMany = useRunCouncilPhases();
  const runAll = useRunAllCouncilPhases();
  const [selected, setSelected] = useState<string[]>([]);

  const phases = useMemo(() => {
    const byName = new Map((phasesQuery.data ?? []).map((p) => [p.phaseName, p]));
    return PHASES_ORDER.map((phaseName) => {
      return (
        byName.get(phaseName) ?? {
          phaseName,
          status: "idle",
          itemsProcessed: 0,
          itemsTotal: 0,
        }
      );
    });
  }, [phasesQuery.data]);

  const isAnyRunning = phases.some((p) => p.status === "running");

  const toggleSelected = (phase: string) => {
    setSelected((prev) => (prev.includes(phase) ? prev.filter((p) => p !== phase) : [...prev, phase]));
  };

  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-sm font-semibold">{t("title")}</h3>
          <p className="text-xs text-muted-foreground">{t("subtitle")}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => {
              if (window.confirm(t("confirmRunAll"))) runAll.mutate();
            }}
            disabled={isAnyRunning || runAll.isPending}
            className="rounded-lg bg-primary px-3 py-2 text-xs font-medium text-primary-foreground disabled:opacity-50"
          >
            {runAll.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : t("runAll")}
          </button>
          <button
            onClick={() => runMany.mutate(selected)}
            disabled={selected.length === 0 || isAnyRunning || runMany.isPending}
            className="rounded-lg border border-border px-3 py-2 text-xs font-medium disabled:opacity-50"
          >
            {t("runSelected")}
          </button>
        </div>
      </div>

      <div className="space-y-2">
        {phases.map((phase) => (
          <PhaseRow
            key={phase.phaseName}
            phase={phase}
            checked={selected.includes(phase.phaseName)}
            onToggle={() => toggleSelected(phase.phaseName)}
            onRun={() => runOne.mutate(phase.phaseName)}
            runningMutation={runOne.isPending && runOne.variables === phase.phaseName}
            disabled={isAnyRunning}
          />
        ))}
      </div>

      <div className="mt-4 border-t border-border pt-3 text-xs text-muted-foreground">
        {cacheQuery.data
          ? t("cacheFiles", { count: cacheQuery.data.totalFiles, size: `${cacheQuery.data.totalSizeMb} MB` })
          : t("cache")}
      </div>
    </div>
  );
}

function PhaseRow({
  phase,
  checked,
  onToggle,
  onRun,
  runningMutation,
  disabled,
}: {
  phase: PhaseStatusDTO;
  checked: boolean;
  onToggle: () => void;
  onRun: () => void;
  runningMutation: boolean;
  disabled: boolean;
}) {
  const t = useTranslations("ingestion.councilIngestion");
  const label = t(`phases.${phase.phaseName}` as "phases.council_seed");
  const progress =
    phase.itemsTotal > 0 ? Math.min(100, Math.round((phase.itemsProcessed / phase.itemsTotal) * 100)) : 0;
  const statusUi = getStatusUi(phase.status, t);

  return (
    <div className="rounded-lg border border-border p-3">
      <div className="flex items-center gap-3">
        <input type="checkbox" checked={checked} onChange={onToggle} disabled={disabled || phase.status === "running"} />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">{label}</p>
          <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
            <span className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 ${statusUi.bg} ${statusUi.fg}`}>
              {statusUi.icon}
              {statusUi.label}
            </span>
            <span>
              {phase.itemsTotal > 0 ? `${phase.itemsProcessed}/${phase.itemsTotal}` : "--"}
            </span>
            <span>{phase.completedAt ?? phase.startedAt ?? t("never")}</span>
          </div>
          {phase.status === "running" && (
            <div className="mt-2 h-1.5 w-full overflow-hidden rounded bg-muted">
              <div className="h-full bg-blue-500 transition-all" style={{ width: `${progress}%` }} />
            </div>
          )}
          {phase.status === "failed" && phase.errorMessage && (
            <details className="mt-2 text-xs text-red-500">
              <summary className="cursor-pointer">{t("errorDetails")}</summary>
              <p className="mt-1 whitespace-pre-wrap">{phase.errorMessage}</p>
            </details>
          )}
        </div>
        <button
          onClick={onRun}
          disabled={disabled || phase.status === "running" || runningMutation}
          className="rounded-lg border border-border p-2 text-muted-foreground hover:bg-accent disabled:opacity-50"
          title={t("runPhase")}
        >
          {runningMutation ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
        </button>
      </div>
    </div>
  );
}

function getStatusUi(status: string, t: ReturnType<typeof useTranslations>) {
  switch (status) {
    case "running":
      return {
        label: t("status.running"),
        bg: "bg-blue-500/15",
        fg: "text-blue-500",
        icon: <Loader2 className="h-3.5 w-3.5 animate-spin" />,
      };
    case "success":
      return {
        label: t("status.success"),
        bg: "bg-emerald-500/15",
        fg: "text-emerald-500",
        icon: <CheckCircle2 className="h-3.5 w-3.5" />,
      };
    case "failed":
      return {
        label: t("status.failed"),
        bg: "bg-red-500/15",
        fg: "text-red-500",
        icon: <AlertCircle className="h-3.5 w-3.5" />,
      };
    default:
      return {
        label: t("status.idle"),
        bg: "bg-muted",
        fg: "text-muted-foreground",
        icon: <Clock3 className="h-3.5 w-3.5" />,
      };
  }
}
