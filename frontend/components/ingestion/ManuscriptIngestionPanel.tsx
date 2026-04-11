"use client";

import { useTranslations } from "next-intl";
import { IngestionPhasePanel } from "./IngestionPhasePanel";
import {
  useManuscriptIngestionPhases,
  useRunManuscriptPhase,
  useRunAllManuscriptPhases,
} from "@/hooks/useIngestionPhases";
import { useResetDomain } from "@/hooks/useIngestion";

const PHASES_ORDER = [
  "manuscript_seed_books",
  "manuscript_ingest",
  "manuscript_coverage",
  "manuscript_enrich_dating",
];

export function ManuscriptIngestionPanel() {
  const t = useTranslations("ingestion.manuscriptIngestion");
  const tr = useTranslations("ingestion.phasePanel");
  const phasesQuery = useManuscriptIngestionPhases();
  const runOne = useRunManuscriptPhase();
  const runAll = useRunAllManuscriptPhases();
  const resetDomain = useResetDomain();

  const phaseLabels: Record<string, string> = {};
  for (const phase of PHASES_ORDER) {
    phaseLabels[phase] = t(`phases.${phase}` as "phases.manuscript_seed_books");
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
      onReset={() => resetDomain.mutate("manuscripts")}
      confirmResetMessage={tr("confirmResetDomain", { domain: t("title") })}
      isResetPending={resetDomain.isPending}
    />
  );
}
