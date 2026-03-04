"use client";

import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";

export default function HistoryPage() {
  const t = useTranslations("history");

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-3xl p-4 md:p-6 space-y-8">
        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">{t("papyriOrigin")}</h2>
          <p className="text-muted-foreground leading-relaxed">
            {t("papyriOriginText")}
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">{t("codexDevelopment")}</h2>
          <p className="text-muted-foreground leading-relaxed">
            {t("codexDevelopmentText")}
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">{t("greatCodex")}</h2>
          <p className="text-muted-foreground leading-relaxed">
            {t("greatCodexText")}
          </p>
        </section>

        <section className="rounded-xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold mb-4">{t("textualExpansion")}</h2>
          <p className="text-muted-foreground leading-relaxed">
            {t("textualExpansionText")}
          </p>
        </section>
      </div>
    </div>
  );
}
