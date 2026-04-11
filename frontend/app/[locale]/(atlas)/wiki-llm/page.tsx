"use client";

import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import {
  ArrowDown,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Zap,
  Shield,
  Database,
  Plus,
} from "lucide-react";

function Code({ children }: { children: React.ReactNode }) {
  return (
    <code className="rounded bg-muted px-1.5 py-0.5 text-sm font-mono">
      {children}
    </code>
  );
}

const colorMap = {
  blue: "border-blue-500/30 bg-blue-500/10 text-blue-700 dark:text-blue-300",
  purple: "border-purple-500/30 bg-purple-500/10 text-purple-700 dark:text-purple-300",
  green: "border-green-500/30 bg-green-500/10 text-green-700 dark:text-green-300",
  yellow: "border-yellow-500/30 bg-yellow-500/10 text-yellow-700 dark:text-yellow-300",
  red: "border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300",
} as const;

function FlowBox({ label, sub, color }: { label: string; sub?: string; color: keyof typeof colorMap }) {
  return (
    <div className={`rounded-lg border px-4 py-2.5 text-center text-sm font-medium ${colorMap[color]}`}>
      {label}
      {sub && <p className="text-xs font-normal opacity-75 mt-0.5">{sub}</p>}
    </div>
  );
}

function Arrow() {
  return (
    <div className="flex justify-center text-muted-foreground">
      <ArrowDown className="h-5 w-5" />
    </div>
  );
}

function ScenarioStep({
  icon,
  label,
  detail,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  detail: string;
  color: string;
}) {
  return (
    <div className="flex items-start gap-3">
      <div className={`mt-0.5 shrink-0 rounded-full p-1 ${color}`}>{icon}</div>
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{detail}</p>
      </div>
    </div>
  );
}

export default function WikiLlmPage() {
  const t = useTranslations("wikiLlm");

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-3xl p-4 md:p-6 space-y-8">
        {/* Section 1 — Overview */}
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">{t("overviewTitle")}</h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              {t.rich("overviewText1", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
              })}
            </p>
            <p>{t("overviewText2")}</p>
            <p>
              {t.rich("overviewText3", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
              })}
            </p>
          </div>
        </section>

        {/* Section 2 — Provider Priority */}
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("providerTitle")}
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-4">
            {t("providerIntro")}
          </p>
          <div className="overflow-x-auto rounded-lg border border-border">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-4 py-3 text-left font-semibold">
                    {t("providerColPriority")}
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    {t("providerColProvider")}
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    {t("providerColModel")}
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    {t("providerColRole")}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-border">
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs font-medium text-green-500">
                      #1
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium">Anthropic</td>
                  <td className="px-4 py-3 text-muted-foreground">
                    <Code>claude-opus-4-6</Code>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("providerRolePrimary")}
                  </td>
                </tr>
                <tr>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-yellow-500/10 px-2.5 py-0.5 text-xs font-medium text-yellow-500">
                      #2
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium">OpenAI</td>
                  <td className="px-4 py-3 text-muted-foreground">
                    <Code>gpt-5.4</Code>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("providerRoleFallback")}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {/* Section 3 — Execution Flow */}
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">{t("flowTitle")}</h2>
          <p className="text-muted-foreground leading-relaxed mb-4">
            {t("flowIntro")}
          </p>
          <ol className="list-decimal list-inside space-y-2 ml-2 text-muted-foreground leading-relaxed">
            <li>{t("flowStep1")}</li>
            <li>
              {t.rich("flowStep2", {
                code: (chunks) => <Code>{chunks}</Code>,
              })}
            </li>
            <li>{t("flowStep3")}</li>
            <li>{t("flowStep4")}</li>
            <li>
              {t.rich("flowStep5", {
                code: (chunks) => <Code>{chunks}</Code>,
              })}
            </li>
            <li>{t("flowStep6")}</li>
          </ol>
        </section>

        {/* Section 4 — Architecture Diagram */}
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">{t("diagramTitle")}</h2>
          <p className="text-muted-foreground leading-relaxed mb-6">
            {t("diagramIntro")}
          </p>
          <div className="flex flex-col items-center gap-2">
            <FlowBox label={t("diagramClient")} color="blue" />
            <Arrow />
            <FlowBox
              label={t("diagramOrchestrator")}
              sub={t("diagramOrchestratorSub")}
              color="purple"
            />
            <Arrow />
            <FlowBox
              label={t("diagramAnthropic")}
              sub={t("diagramAnthropicSub")}
              color="green"
            />
            <div className="flex flex-col sm:flex-row items-center gap-3 my-2 w-full justify-center">
              <div className="flex items-center gap-2 rounded-lg border border-green-500/30 bg-green-500/5 px-3 py-1.5 text-xs text-green-600 dark:text-green-400">
                <CheckCircle2 className="h-3.5 w-3.5" />
                {t("diagramSuccess")}
              </div>
              <span className="text-muted-foreground text-xs">{t("diagramOr")}</span>
              <div className="flex items-center gap-2 rounded-lg border border-yellow-500/30 bg-yellow-500/5 px-3 py-1.5 text-xs text-yellow-600 dark:text-yellow-400">
                <AlertTriangle className="h-3.5 w-3.5" />
                {t("diagramFallback")}
              </div>
            </div>
            <Arrow />
            <FlowBox
              label={t("diagramOpenai")}
              sub={t("diagramOpenaiSub")}
              color="yellow"
            />
            <div className="flex flex-col sm:flex-row items-center gap-3 my-2 w-full justify-center">
              <div className="flex items-center gap-2 rounded-lg border border-green-500/30 bg-green-500/5 px-3 py-1.5 text-xs text-green-600 dark:text-green-400">
                <CheckCircle2 className="h-3.5 w-3.5" />
                {t("diagramSuccess")}
              </div>
              <span className="text-muted-foreground text-xs">{t("diagramOr")}</span>
              <div className="flex items-center gap-2 rounded-lg border border-red-500/30 bg-red-500/5 px-3 py-1.5 text-xs text-red-600 dark:text-red-400">
                <XCircle className="h-3.5 w-3.5" />
                {t("diagramException")}
              </div>
            </div>
          </div>
        </section>

        {/* Section 5 — Scenarios */}
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">{t("scenarioTitle")}</h2>

          {/* Scenario A */}
          <div className="mb-6">
            <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-500" />
              {t("scenarioATitle")}
            </h3>
            <div className="space-y-3 ml-1 border-l-2 border-green-500/30 pl-4">
              <ScenarioStep
                icon={<Zap className="h-3 w-3 text-blue-500" />}
                color="bg-blue-500/10"
                label={t("scenarioAStep1Label")}
                detail={t("scenarioAStep1Detail")}
              />
              <ScenarioStep
                icon={<Shield className="h-3 w-3 text-purple-500" />}
                color="bg-purple-500/10"
                label={t("scenarioAStep2Label")}
                detail={t("scenarioAStep2Detail")}
              />
              <ScenarioStep
                icon={<CheckCircle2 className="h-3 w-3 text-green-500" />}
                color="bg-green-500/10"
                label={t("scenarioAStep3Label")}
                detail={t("scenarioAStep3Detail")}
              />
            </div>
          </div>

          {/* Scenario B */}
          <div>
            <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-yellow-500" />
              {t("scenarioBTitle")}
            </h3>
            <div className="space-y-3 ml-1 border-l-2 border-yellow-500/30 pl-4">
              <ScenarioStep
                icon={<Zap className="h-3 w-3 text-blue-500" />}
                color="bg-blue-500/10"
                label={t("scenarioBStep1Label")}
                detail={t("scenarioBStep1Detail")}
              />
              <ScenarioStep
                icon={<XCircle className="h-3 w-3 text-red-500" />}
                color="bg-red-500/10"
                label={t("scenarioBStep2Label")}
                detail={t("scenarioBStep2Detail")}
              />
              <ScenarioStep
                icon={<AlertTriangle className="h-3 w-3 text-yellow-500" />}
                color="bg-yellow-500/10"
                label={t("scenarioBStep3Label")}
                detail={t("scenarioBStep3Detail")}
              />
              <ScenarioStep
                icon={<CheckCircle2 className="h-3 w-3 text-green-500" />}
                color="bg-green-500/10"
                label={t("scenarioBStep4Label")}
                detail={t("scenarioBStep4Detail")}
              />
            </div>
          </div>
        </section>

        {/* Section 6 — Error Handling */}
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">{t("errorTitle")}</h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              {t.rich("errorText1", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
                code: (chunks) => <Code>{chunks}</Code>,
              })}
            </p>
            <p>{t("errorText2")}</p>
            <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
              <p className="font-medium mb-1">{t("errorNonRetryableTitle")}</p>
              <p className="text-xs opacity-80">{t("errorNonRetryableText")}</p>
            </div>
            <div className="rounded-lg border border-border bg-muted/30 p-4">
              <p className="text-sm font-medium text-foreground mb-2">{t("errorGracefulTitle")}</p>
              <ul className="list-disc list-inside space-y-1 text-sm">
                <li>{t("errorGraceful1")}</li>
                <li>{t("errorGraceful2")}</li>
                <li>{t("errorGraceful3")}</li>
                <li>{t("errorGraceful4")}</li>
              </ul>
            </div>
          </div>
        </section>

        {/* Section 7 — Extensibility */}
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <Plus className="h-5 w-5 text-muted-foreground" />
            {t("extensibilityTitle")}
          </h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              {t.rich("extensibilityText1", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
              })}
            </p>
            <ol className="list-decimal list-inside space-y-2 ml-2">
              <li>
                {t.rich("extensibilityStep1", {
                  code: (chunks) => <Code>{chunks}</Code>,
                })}
              </li>
              <li>
                {t.rich("extensibilityStep2", {
                  code: (chunks) => <Code>{chunks}</Code>,
                })}
              </li>
              <li>
                {t.rich("extensibilityStep3", {
                  code: (chunks) => <Code>{chunks}</Code>,
                })}
              </li>
              <li>{t("extensibilityStep4")}</li>
            </ol>
            <div className="rounded-lg border border-border bg-muted/30 p-4 mt-2">
              <div className="flex items-start gap-2">
                <Database className="h-4 w-4 text-muted-foreground mt-0.5 shrink-0" />
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {t("extensibilityDbTitle")}
                  </p>
                  <p className="text-xs mt-1">{t("extensibilityDbText")}</p>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
