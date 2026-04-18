"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { IngestionPhasePanel } from "./IngestionPhasePanel";
import { BibleLayer4ScopePanel } from "./BibleLayer4ScopePanel";
import {
  useBibleIngestionPhases,
  useRunBiblePhase,
  useClearBibleGlosses,
} from "@/hooks/useIngestionPhases";
import { useRunBibleLayerPhases } from "@/hooks/useIngestionPhases";
import { useResetDomain } from "@/hooks/useIngestion";
import {
  Eraser,
  Loader2,
  Database,
  Languages,
  BookOpen,
  GitBranch,
  ChevronDown,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";
import type { PhaseStatusDTO } from "@/types";

const LAYER_1_PHASES = [
  "bible_seed_versions",
  "bible_seed_books",
  "bible_seed_abbreviations",
];

const LAYER_2_PHASES = [
  "bible_ingest_nt_interlinear",
  "bible_ingest_ot_interlinear",
  "bible_ingest_greek_lexicon",
  "bible_ingest_hebrew_lexicon",
  "bible_fill_missing_hebrew",
  "bible_translate_lexicon",
  "bible_translate_hebrew_lexicon",
  "bible_translate_glosses",
  "bible_enrich_greek_lexicon",
  "bible_enrich_hebrew_lexicon",
  "bible_reenrich_greek_lexicon",
  "bible_reenrich_hebrew_lexicon",
  "bible_translate_enrichment_greek",
  "bible_translate_enrichment_hebrew",
];

const LAYER_3_PHASES = [
  "bible_ingest_text_kjv",
  "bible_ingest_text_aa",
  "bible_ingest_text_acf",
  "bible_ingest_text_arc69",
];

const LAYER_4_PHASES = [
  "bible_tokenize_arc69",
  "bible_tokenize_kjv",
  "bible_lemmatize_arc69",
  "bible_lemmatize_kjv",
  "bible_align_kjv",
  "bible_align_arc69",
  "bible_enrich_semantics_arc69",
  "bible_align_hebrew_kjv",
  "bible_align_hebrew_arc69",
];

const ALL_PHASES = [...LAYER_1_PHASES, ...LAYER_2_PHASES, ...LAYER_3_PHASES, ...LAYER_4_PHASES];

export function BibleIngestionPanel() {
  const t = useTranslations("ingestion.bibleIngestion");
  const tr = useTranslations("ingestion.phasePanel");
  const phasesQuery = useBibleIngestionPhases();
  const runOne = useRunBiblePhase();
  const runLayer = useRunBibleLayerPhases();
  const resetDomain = useResetDomain();
  const clearGlosses = useClearBibleGlosses();
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const toggle = (layer: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(layer)) next.delete(layer);
      else next.add(layer);
      return next;
    });
  };

  const phaseLabels: Record<string, string> = {};
  const phaseDescriptions: Record<string, string> = {};
  for (const phase of ALL_PHASES) {
    phaseLabels[phase] = t(`phases.${phase}` as "phases.bible_seed_versions");
    phaseDescriptions[phase] = t(`phaseDescriptions.${phase}` as "phaseDescriptions.bible_seed_versions");
  }

  const layers = [
    {
      key: "layer1",
      icon: <Database className="h-4 w-4" />,
      phases: LAYER_1_PHASES,
      resetDomain: "bible-layer1",
      footer: undefined,
    },
    {
      key: "layer2",
      icon: <Languages className="h-4 w-4" />,
      phases: LAYER_2_PHASES,
      resetDomain: "bible-layer2",
      footer: (
        <button
          onClick={() => {
            if (window.confirm(t("confirmClearGlosses"))) clearGlosses.mutate();
          }}
          disabled={clearGlosses.isPending}
          className="inline-flex items-center gap-1.5 rounded-lg border border-amber-500/50 px-3 py-1.5 text-xs font-medium text-amber-400 hover:bg-amber-500/10 disabled:opacity-50"
        >
          {clearGlosses.isPending ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Eraser className="h-3.5 w-3.5" />}
          {t("clearGlosses")}
        </button>
      ),
    },
    {
      key: "layer3",
      icon: <BookOpen className="h-4 w-4" />,
      phases: LAYER_3_PHASES,
      resetDomain: "bible-layer3",
      footer: undefined,
    },
    {
      key: "layer4",
      icon: <GitBranch className="h-4 w-4" />,
      phases: LAYER_4_PHASES,
      resetDomain: "bible-layer4",
      footer: undefined,
    },
  ];

  return (
    <div className="space-y-4">
      {layers.map((layer, idx) => {
        const layerNum = idx + 1;
        const layerPhases = phasesQuery.data?.filter((p) => layer.phases.includes(p.phaseName));
        return (
          <LayerSection
            key={layer.key}
            icon={layer.icon}
            title={t(`layer${layerNum}Title` as "layer1Title")}
            subtitle={t(`layer${layerNum}Subtitle` as "layer1Subtitle")}
            description={t(`layer${layerNum}Description` as "layer1Description")}
            dependsOn={t(`layer${layerNum}Depends` as "layer1Depends")}
            source={t(`layer${layerNum}Source` as "layer1Source")}
            phasesData={layerPhases}
            totalPhases={layer.phases.length}
            expanded={expanded.has(layer.key)}
            onToggle={() => toggle(layer.key)}
          >
            {layer.key === "layer4" && (
              <BibleLayer4ScopePanel phases={layer.phases} phaseLabels={phaseLabels} />
            )}
            <IngestionPhasePanel
              phasesOrder={layer.phases}
              phasesData={layerPhases}
              phaseLabels={phaseLabels}
              phaseDescriptions={phaseDescriptions}
              confirmRunAllMessage={t("confirmRunLayer")}
              onRunPhase={(phase) => runOne.mutate(phase)}
              onRunAll={() => runLayer.mutate(layer.phases)}
              isRunPhasePending={runOne.isPending}
              runPhaseVariable={runOne.variables}
              isRunAllPending={runLayer.isPending}
              onReset={() => resetDomain.mutate(layer.resetDomain as Parameters<typeof resetDomain.mutate>[0])}
              confirmResetMessage={tr("confirmResetDomain", { domain: t(`layer${layerNum}Title` as "layer1Title") })}
              isResetPending={resetDomain.isPending}
              footer={layer.footer}
            />
          </LayerSection>
        );
      })}
    </div>
  );
}

function LayerSection({
  icon,
  title,
  subtitle,
  description,
  dependsOn,
  source,
  phasesData,
  totalPhases,
  expanded,
  onToggle,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  description: string;
  dependsOn: string;
  source: string;
  phasesData: PhaseStatusDTO[] | undefined;
  totalPhases: number;
  expanded: boolean;
  onToggle: () => void;
  children: React.ReactNode;
}) {
  const completedCount = phasesData?.filter((p) => p.status === "success").length ?? 0;
  const failedCount = phasesData?.filter((p) => p.status === "failed").length ?? 0;

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      {/* Header — clicável */}
      <button
        onClick={onToggle}
        className="flex w-full items-center gap-3 p-4 text-left transition-colors hover:bg-accent/50"
      >
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
          {icon}
        </div>
        <div className="min-w-0 flex-1">
          <h3 className="text-sm font-bold">{title}</h3>
          <p className="truncate text-xs text-muted-foreground">{subtitle}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {completedCount > 0 && (
            <span className="inline-flex items-center gap-1 text-xs text-emerald-500">
              <CheckCircle2 className="h-3 w-3" />
              {completedCount}/{totalPhases}
            </span>
          )}
          {failedCount > 0 && (
            <span className="inline-flex items-center gap-1 text-xs text-red-500">
              <AlertCircle className="h-3 w-3" />
              {failedCount}
            </span>
          )}
          <ChevronDown
            className={`h-4 w-4 text-muted-foreground transition-transform duration-200 ${expanded ? "rotate-0" : "-rotate-90"}`}
          />
        </div>
      </button>

      {/* Corpo expandível */}
      <div
        className={`grid overflow-hidden transition-[grid-template-rows,opacity] duration-300 ${expanded ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"}`}
      >
        <div className="min-h-0">
          <div className="space-y-4 border-t border-border p-4">
            {/* Descrição da camada */}
            <p className="text-sm leading-relaxed text-muted-foreground">{description}</p>

            {/* Info badges */}
            <div className="flex flex-wrap gap-3 text-xs">
              <span className="inline-flex items-center gap-1.5 rounded-md bg-muted px-2.5 py-1">
                <span className="font-medium text-foreground/70">Depende:</span>
                <span className="text-muted-foreground">{dependsOn}</span>
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-md bg-muted px-2.5 py-1">
                <span className="font-medium text-foreground/70">Fonte:</span>
                <span className="text-muted-foreground">{source}</span>
              </span>
            </div>

            {/* Fases */}
            {children}
          </div>
        </div>
      </div>
    </div>
  );
}
