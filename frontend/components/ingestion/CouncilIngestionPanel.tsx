"use client";

import { useTranslations } from "next-intl";
import { IngestionPhasePanel } from "./IngestionPhasePanel";
import {
  useCouncilIngestionPhases,
  useRunCouncilPhase,
  useRunAllCouncilPhases,
  useCouncilIngestionCache,
} from "@/hooks/useCouncilIngestion";
import { useResetDomain } from "@/hooks/useIngestion";

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
  const tr = useTranslations("ingestion.phasePanel");
  const phasesQuery = useCouncilIngestionPhases();
  const cacheQuery = useCouncilIngestionCache();
  const runOne = useRunCouncilPhase();
  const runAll = useRunAllCouncilPhases();
  const resetDomain = useResetDomain();

  const phaseLabels: Record<string, string> = {};
  for (const phase of PHASES_ORDER) {
    phaseLabels[phase] = t(`phases.${phase}` as "phases.council_seed");
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
      onReset={() => resetDomain.mutate("councils")}
      confirmResetMessage={tr("confirmResetDomain", { domain: t("title") })}
      isResetPending={resetDomain.isPending}
      footer={
        cacheQuery.data
          ? t("cacheFiles", { count: cacheQuery.data.totalFiles, size: `${cacheQuery.data.totalSizeMb} MB` })
          : null
      }
    />
  );
}
