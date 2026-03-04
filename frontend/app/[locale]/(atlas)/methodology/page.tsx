"use client";

import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";

function Code({ children }: { children: React.ReactNode }) {
  return (
    <code className="rounded bg-muted px-1.5 py-0.5 text-sm font-mono">
      {children}
    </code>
  );
}

export default function MethodologyPage() {
  const t = useTranslations("methodology");

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-3xl p-4 md:p-6 space-y-8">
        {/* Section 1 — Overview */}
        <section className="rounded-xl border border-border bg-card p-6">
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
            <p>{t("overviewText3")}</p>
          </div>
        </section>

        {/* Section 2 — Manuscript Sources */}
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("manuscriptSourcesTitle")}
          </h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              {t.rich("manuscriptSourcesText1", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
              })}
            </p>
            <p>
              {t("manuscriptSourcesText2", {
                origEarly: "origEarly",
                origLate: "origLate",
              })}
            </p>
            <p>
              {t("manuscriptSourcesText3", {
                yearMin: "yearMin",
                yearMax: "yearMax",
                confidenceHigh: "datingConfidence = HIGH",
              })}
            </p>
            <p className="font-medium text-foreground">
              {t("manuscriptSourcesText4")}
            </p>
          </div>
        </section>

        {/* Section 3 — Patristic Sources */}
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("patristicSourcesTitle")}
          </h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>{t("patristicSourcesText1")}</p>
            <p>
              {t.rich("patristicSourcesText2", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
              })}
            </p>
            <p>
              {t("patristicSourcesText3", {
                confidenceLow: "datingConfidence = LOW",
              })}
            </p>
            <p>
              {t.rich("patristicSourcesText4", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
              })}
            </p>
          </div>
        </section>

        {/* Section 4 — Confidence Levels */}
        <section id="confidence" className="rounded-xl border border-border bg-card p-6 scroll-mt-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("confidenceTitle")}
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-4">
            {t("confidenceIntro")}
          </p>
          <div className="overflow-x-auto rounded-lg border border-border">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-4 py-3 text-left font-semibold">
                    {t("confidenceLevel")}
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    {t("confidenceSource")}
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    {t("confidenceDescription")}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-border">
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs font-medium text-green-500">
                      {t("confidenceHighLabel")}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("confidenceHighSource")}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("confidenceHighDesc")}
                  </td>
                </tr>
                <tr className="border-b border-border">
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-yellow-500/10 px-2.5 py-0.5 text-xs font-medium text-yellow-500">
                      {t("confidenceMediumLabel")}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("confidenceMediumSource")}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("confidenceMediumDesc")}
                  </td>
                </tr>
                <tr>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-500">
                      {t("confidenceLowLabel")}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("confidenceLowSource")}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {t("confidenceLowDesc")}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p className="mt-4 text-sm text-muted-foreground">
            {t.rich("confidenceRule", {
              strong: (chunks) => (
                <strong className="text-foreground">{chunks}</strong>
              ),
            })}
          </p>
        </section>

        {/* Section 5 — yearBest */}
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">{t("yearBestTitle")}</h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              {t("yearBestText1", {
                yearBest: "yearBest",
                example: "yearBest = 180",
              })}
            </p>
            <p>
              {t.rich("yearBestText2", {
                strong: (chunks) => (
                  <strong className="text-foreground">{chunks}</strong>
                ),
              })}
            </p>
            <p>{t("yearBestText3")}</p>
          </div>
        </section>

        {/* Section 6 — Enrichment Process */}
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("enrichmentTitle")}
          </h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>{t("enrichmentIntro")}</p>
            <ol className="list-decimal list-inside space-y-2 ml-2">
              <li>
                {t("enrichmentStep1", {
                  endpoint: "POST /admin/enrich-dating",
                })}
              </li>
              <li>
                {t("enrichmentStep2", { yearMin: "year_min" })}
              </li>
              <li>{t("enrichmentStep3")}</li>
              <li>{t("enrichmentStep4")}</li>
              <li>
                {t("enrichmentStep5", {
                  source: "datingSource = openai",
                  confidence: "datingConfidence = LOW",
                })}
              </li>
              <li>{t("enrichmentStep6")}</li>
            </ol>
          </div>
        </section>
      </div>
    </div>
  );
}
