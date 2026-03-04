"use client";

import { useParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { Link } from "@/i18n/navigation";
import { useHeresyDetail } from "@/hooks/useHeresies";
import { ArrowLeft } from "lucide-react";
import { CouncilTypeBadge } from "@/components/councils/CouncilTypeBadge";

export default function HeresyDetailPage() {
  const params = useParams();
  const slug = String(params.slug ?? "");
  const t = useTranslations("heresies");
  const tc = useTranslations("common");

  const query = useHeresyDetail(slug);

  if (query.isLoading) {
    return (
      <div className="min-h-screen">
        <Header title={t("detailTitle")} subtitle={tc("loading")} />
      </div>
    );
  }

  if (query.error || !query.data) {
    return (
      <div className="min-h-screen">
        <Header title={tc("error")} subtitle="" />
        <div className="p-4 md:p-6 text-red-500">{(query.error as Error)?.message ?? t("notFound")}</div>
      </div>
    );
  }

  const heresy = query.data;

  return (
    <div className="min-h-screen">
      <Header title={heresy.name} subtitle={t("detailSubtitle")} />

      <div className="space-y-6 p-4 md:p-6">
        <Link href="/heresies" className="inline-flex items-center gap-2 rounded-lg p-2 text-sm text-muted-foreground hover:bg-secondary">
          <ArrowLeft className="h-4 w-4" />
          {t("backToList")}
        </Link>

        <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
          <div className="space-y-4 rounded-xl border border-border bg-card p-4 md:p-6">
            <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
              <span>{t("yearOrigin")}: {heresy.yearOrigin ?? "—"}</span>
              <span>{t("centuryOrigin")}: {heresy.centuryOrigin ?? "—"}</span>
            </div>

            {heresy.description ? (
              <p className="whitespace-pre-wrap text-sm leading-relaxed">{heresy.description}</p>
            ) : (
              <p className="text-sm text-muted-foreground">{t("noDescription")}</p>
            )}

            {heresy.wikipediaUrl && (
              <a
                href={heresy.wikipediaUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex rounded-lg border border-border px-3 py-2 text-sm hover:bg-secondary"
              >
                {t("openWikipedia")}
              </a>
            )}
          </div>

          <aside className="rounded-xl border border-border bg-card p-4">
            <h3 className="text-sm font-semibold">{t("quickInfo")}</h3>
            <div className="mt-3 space-y-2 text-sm">
              <p><span className="text-muted-foreground">{t("yearOrigin")}:</span> {heresy.yearOrigin ?? "—"}</p>
              <p><span className="text-muted-foreground">{t("centuryOrigin")}:</span> {heresy.centuryOrigin ?? "—"}</p>
              <p><span className="text-muted-foreground">{t("councilsCount")}:</span> {heresy.councils.length}</p>
            </div>
          </aside>
        </div>

        <div className="rounded-xl border border-border bg-card p-4 md:p-6">
          <h2 className="mb-4 text-base font-semibold">{t("condemningCouncils")}</h2>
          {heresy.councils.length === 0 && <p className="text-sm text-muted-foreground">{t("noCouncils")}</p>}
          {heresy.councils.length > 0 && (
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
              {heresy.councils.map((c) => (
                <Link key={c.id} href={`/councils/${c.slug}`} className="rounded-lg border border-border p-3 hover:bg-secondary/30">
                  <p className="font-medium">{c.displayName}</p>
                  <p className="text-xs text-muted-foreground">{c.year}{c.yearEnd ? `-${c.yearEnd}` : ""}</p>
                  <div className="mt-2">
                    <CouncilTypeBadge type={c.councilType} />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
