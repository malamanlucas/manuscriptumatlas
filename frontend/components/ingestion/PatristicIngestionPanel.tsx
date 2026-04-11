"use client";

import { useTranslations } from "next-intl";
import { IngestionPhasePanel } from "./IngestionPhasePanel";
import {
  usePatristicIngestionPhases,
  useRunPatristicPhase,
  useRunAllPatristicPhases,
} from "@/hooks/useIngestionPhases";
import { useResetDomain } from "@/hooks/useIngestion";

const PHASES_ORDER = [
  "patristic_seed_fathers",
  "patristic_seed_statements",
  "patristic_translate_fathers",
  "patristic_translate_statements",
  "patristic_translate_biographies",
  "patristic_enrich_dating",
];

export function PatristicIngestionPanel() {
  const t = useTranslations("ingestion.patristicIngestion");
  const tr = useTranslations("ingestion.phasePanel");
  const phasesQuery = usePatristicIngestionPhases();
  const runOne = useRunPatristicPhase();
  const runAll = useRunAllPatristicPhases();
  const resetDomain = useResetDomain();

  const phaseLabels: Record<string, string> = {};
  for (const phase of PHASES_ORDER) {
    phaseLabels[phase] = t(`phases.${phase}` as "phases.patristic_seed_fathers");
  }

  return (
    <IngestionPhasePanel
      phasesOrder={PHASES_ORDER}
      phasesData={phasesQuery.data}
      phaseLabels={phaseLabels}
      confirmRunAllMessage={t("confirmRunAll")}
      onRunPhase={(phase) => runOne.mutate(phase)}
      onRunAll={() => runAll.mutate()}
      isRunPhasePending={runOne.isPending}
      runPhaseVariable={runOne.variables}
      isRunAllPending={runAll.isPending}
      onReset={() => resetDomain.mutate("patristic")}
      confirmResetMessage={tr("confirmResetDomain", { domain: t("title") })}
      isResetPending={resetDomain.isPending}
      resetSuccess={resetDomain.isSuccess}
    />
  );
}
