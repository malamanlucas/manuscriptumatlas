"use client";

import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";

export default function SourcesPage() {
  const t = useTranslations("sources");

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-3xl p-4 md:p-6 space-y-8">
        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("primarySource")}
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-3">
            {t("primarySourceText1", { ntvmr: t("ntvmrName") })}
          </p>
          <p className="text-muted-foreground leading-relaxed mb-3">
            {t("primarySourceText2", {
              formatParam: "format=teiraw",
              abElement: "<ab>",
            })}
          </p>
          <p className="text-muted-foreground leading-relaxed">
            {t("primarySourceText3")}
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("gregoryAland")}
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-3">
            {t("gregoryAlandText1")}
          </p>
          <ul className="list-disc list-inside text-muted-foreground space-y-1 ml-2">
            <li>{t("gregoryAlandPapyri")}</li>
            <li>{t("gregoryAlandUncials")}</li>
            <li>{t("gregoryAlandMinuscules")}</li>
            <li>{t("gregoryAlandLectionaries")}</li>
          </ul>
          <p className="text-muted-foreground leading-relaxed mt-3">
            {t("gregoryAlandText2")}
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("datingMethodology")}
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-3">
            {t("datingMethodologyText1", {
              conservative: t("conservative"),
              centuryMin: "centuryMin",
            })}
          </p>
          <p className="text-muted-foreground leading-relaxed">
            {t("datingMethodologyText2", {
              cumulativeCoverage: t("cumulativeCoverage"),
            })}
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="text-lg font-semibold mb-4">
            {t("limitations")}
          </h2>
          <div className="space-y-3 text-muted-foreground leading-relaxed">
            <p>
              <strong className="text-foreground">{t("limitationFragmentaryLabel")}</strong>{" "}
              {t("limitationFragmentary")}
            </p>
            <p>
              <strong className="text-foreground">{t("limitationDatingLabel")}</strong>{" "}
              {t("limitationDating")}
            </p>
            <p>
              <strong className="text-foreground">{t("limitationVariantsLabel")}</strong>{" "}
              {t("limitationVariants", {
                coverage: t("coverageTerm"),
                textualVariation: t("textualVariationTerm"),
              })}
            </p>
          </div>
        </section>

        <section id="councilSources" className="rounded-xl border border-border bg-card p-4 md:p-6 scroll-mt-24">
          <h2 className="text-lg font-semibold mb-4">
            {t("councilSources")}
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-4">
            {t("councilSourcesIntro")}
          </p>
          <div className="space-y-3">
            <div>
              <h3 className="text-sm font-semibold mb-1">{t("curatedSeedTitle")}</h3>
              <p className="text-muted-foreground text-sm leading-relaxed">
                {t("curatedSeedDescription")}
              </p>
            </div>
            <p className="text-muted-foreground text-sm leading-relaxed">
              {t("councilSourcesOutro")}
            </p>
          </div>
        </section>
      </div>
    </div>
  );
}
