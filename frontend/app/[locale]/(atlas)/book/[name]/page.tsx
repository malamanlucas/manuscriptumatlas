"use client";

import { useState } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { CenturySlider } from "@/components/coverage/CenturySlider";
import { VerseGrid } from "@/components/coverage/VerseGrid";
import { Heatmap } from "@/components/charts/Heatmap";
import { TimelineChart } from "@/components/charts/TimelineChart";
import { useChapterCoverage } from "@/hooks/useCoverage";
import { useTimeline } from "@/hooks/useTimeline";
import { useMissingVerses } from "@/hooks/useMissing";
import { toRoman } from "@/lib/utils";
import { ArrowLeft } from "lucide-react";
import { Link } from "@/i18n/navigation";

export default function BookDetailPage() {
  const t = useTranslations("bookDetail");
  const tc = useTranslations("common");
  const tb = useTranslations("books");

  const params = useParams();
  const searchParams = useSearchParams();
  const bookName = decodeURIComponent(params.name as string);
  const initialCentury = Number(searchParams.get("century")) || 5;
  const [century, setCentury] = useState(initialCentury);
  const [type, setType] = useState<string | undefined>(undefined);

  const translatedBookName = tb(bookName as Parameters<typeof tb>[0]);

  const { data: chapterData, isLoading: chaptersLoading } =
    useChapterCoverage(bookName, century, type);
  const { data: prevChapterData } = useChapterCoverage(
    bookName,
    Math.max(1, century - 1),
    type
  );
  const { data: timelineData } = useTimeline(bookName, type);
  const { data: missingData } = useMissingVerses(bookName, century, type);

  return (
    <div className="min-h-screen">
      <Header
        title={translatedBookName}
        subtitle={t("coverageAsOf", { century: toRoman(century) })}
      />

      <div className="p-6 space-y-6">
        <div className="flex items-center gap-4">
          <Link
            href="/dashboard"
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            {tc("back")}
          </Link>
        </div>

        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div className="flex-1">
              <CenturySlider value={century} onChange={setCentury} />
            </div>
            <div className="flex gap-2">
              {[undefined, "papyrus", "uncial"].map((t_type) => (
                <button
                  key={t_type ?? "all"}
                  onClick={() => setType(t_type)}
                  className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                    type === t_type
                      ? "bg-primary text-primary-foreground"
                      : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
                >
                  {t_type === undefined
                    ? tc("all")
                    : t_type === "papyrus"
                      ? tc("papyri")
                      : tc("uncials")}
                </button>
              ))}
            </div>
          </div>
        </div>

        {chaptersLoading && (
          <div className="animate-pulse space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-20 rounded-xl bg-secondary" />
            ))}
          </div>
        )}

        {chapterData && (
          <>
            <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
              <div className="rounded-xl border border-border bg-card p-6">
                <Heatmap
                  chapters={chapterData.chapters}
                  bookName={bookName}
                />
              </div>

              {timelineData && (
                <div className="rounded-xl border border-border bg-card p-6">
                  <h3 className="mb-3 text-sm font-semibold">
                    {t("coverageTimeline", { book: translatedBookName })}
                  </h3>
                  <TimelineChart entries={timelineData.entries} />
                </div>
              )}
            </div>

            {missingData && missingData.totalMissing > 0 && (
              <div className="rounded-xl border border-border bg-card p-6">
                <h3 className="mb-1 text-sm font-semibold">
                  {t("missingVerses", { count: missingData.totalMissing })}
                </h3>
                <p className="mb-3 text-xs text-muted-foreground">
                  {t("missingVersesDescription", { century: toRoman(century) })}
                </p>
                <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto">
                  {missingData.missingVerses.map((v) => (
                    <span
                      key={`${v.chapter}:${v.verse}`}
                      className="rounded bg-red-100 px-2 py-0.5 text-xs font-mono text-red-700 dark:bg-red-900 dark:text-red-300"
                    >
                      {v.chapter}:{v.verse}
                    </span>
                  ))}
                </div>
              </div>
            )}

            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="mb-4 text-sm font-semibold">
                {t("verseMapByChapter")}
              </h3>
              <VerseGrid
                chapters={chapterData.chapters}
                previousChapters={prevChapterData?.chapters}
                bookName={bookName}
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
