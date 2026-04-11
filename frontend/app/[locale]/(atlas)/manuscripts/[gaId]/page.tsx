"use client";

import { useParams } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useManuscriptDetail } from "@/hooks/useManuscripts";
import { toRoman } from "@/lib/utils";
import { ArrowLeft, ExternalLink } from "lucide-react";
import { DatingBadge } from "@/components/ui/DatingBadge";

export default function ManuscriptDetailPage() {
  const t = useTranslations("manuscriptDetail");
  const tc = useTranslations("common");
  const tBooks = useTranslations("books");

  const params = useParams();
  const gaId = params.gaId as string;

  const { data, isLoading, error } = useManuscriptDetail(gaId);

  return (
    <div className="min-h-screen">
      <Header
        title={gaId}
        subtitle={data?.name ?? t("subtitle")}
      />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
        <Link
          href="/manuscripts"
          className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" /> {t("backToExplorer")}
        </Link>

        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            {t("loadingManuscript")}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        {data && (
          <div className="space-y-6">
            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                <div>
                  <p className="text-xs text-muted-foreground">{tc("gaId")}</p>
                  <p className="font-mono font-bold">{data.gaId}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("century")}</p>
                  <p>
                    {data.centuryMin === data.centuryMax
                      ? toRoman(data.centuryMin)
                      : `${toRoman(data.centuryMin)}–${toRoman(data.centuryMax)}`}
                  </p>
                </div>
                {(data.yearMin || data.yearMax || data.yearBest) && (
                  <div>
                    <p className="text-xs text-muted-foreground">{t("dating")}</p>
                    <DatingBadge
                      yearMin={data.yearMin}
                      yearMax={data.yearMax}
                      yearBest={data.yearBest}
                      datingSource={data.datingSource}
                      datingConfidence={data.datingConfidence}
                      datingReference={data.datingReference}
                    />
                  </div>
                )}
                <div>
                  <p className="text-xs text-muted-foreground">{t("type")}</p>
                  <p className="capitalize">{data.manuscriptType ?? "—"}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">{t("dataSource")}</p>
                  <p>{data.dataSource}</p>
                </div>
              </div>
              {data.historicalNotes && (
                <div className="mt-4 pt-4 border-t border-border">
                  <p className="text-xs text-muted-foreground mb-2">{t("historicalNotes")}</p>
                  <p className="text-sm">{data.historicalNotes}</p>
                </div>
              )}
              <div className="mt-4 pt-4 border-t border-border">
                <a
                  href={data.ntvmrUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline"
                >
                  {t("viewInNtvmr")} <ExternalLink className="h-4 w-4" />
                </a>
              </div>
            </div>

            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <h3 className="text-base font-semibold mb-4">{t("booksPreserved")}</h3>
              <div className="space-y-4">
                {data.booksPreserved.map((br) => (
                  <div key={br.book} className="border-b border-border last:border-0 pb-4 last:pb-0">
                    <p className="font-medium">{tBooks(br.book)}</p>
                    <p className="text-sm text-muted-foreground font-mono mt-1">
                      {br.ranges.join(", ")}
                    </p>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <h3 className="text-base font-semibold mb-4">{t("intervalsSummary")}</h3>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="px-4 py-2 text-left text-sm font-medium">{tc("book")}</th>
                      <th className="px-4 py-2 text-left text-sm font-medium">{t("chapters")}</th>
                      <th className="px-4 py-2 text-right text-sm font-medium">{t("verses")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.intervals.map((i) => (
                      <tr key={i.book} className="border-b border-border last:border-0">
                        <td className="px-4 py-2">{tBooks(i.book)}</td>
                        <td className="px-4 py-2">
                          {i.chapterMin === i.chapterMax
                            ? i.chapterMin
                            : `${i.chapterMin}–${i.chapterMax}`}
                        </td>
                        <td className="px-4 py-2 text-right">{i.verseCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
