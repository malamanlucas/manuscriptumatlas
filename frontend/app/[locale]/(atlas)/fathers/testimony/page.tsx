"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import {
  useTextualStatements,
  useSearchStatements,
  useTopicsSummary,
} from "@/hooks/useTextualStatements";
import { Link } from "@/i18n/navigation";
import { toRoman } from "@/lib/utils";
import { YearRangeFilter } from "@/components/filters/YearRangeFilter";
import { Search, ArrowLeft } from "lucide-react";
import type { TextualTopic } from "@/types";

const ALL_TOPICS: TextualTopic[] = [
  "MANUSCRIPTS",
  "AUTOGRAPHS",
  "APOCRYPHA",
  "CANON",
  "TEXTUAL_VARIANTS",
  "TRANSLATION",
  "CORRUPTION",
  "SCRIPTURE_AUTHORITY",
];

const TOPIC_COLORS: Record<string, string> = {
  MANUSCRIPTS: "bg-blue-500/15 text-blue-400 border-blue-500/30",
  AUTOGRAPHS: "bg-amber-500/15 text-amber-400 border-amber-500/30",
  APOCRYPHA: "bg-red-500/15 text-red-400 border-red-500/30",
  CANON: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
  TEXTUAL_VARIANTS: "bg-purple-500/15 text-purple-400 border-purple-500/30",
  TRANSLATION: "bg-cyan-500/15 text-cyan-400 border-cyan-500/30",
  CORRUPTION: "bg-orange-500/15 text-orange-400 border-orange-500/30",
  SCRIPTURE_AUTHORITY: "bg-indigo-500/15 text-indigo-400 border-indigo-500/30",
};

export default function TestimonyPage() {
  const t = useTranslations("fathers");
  const tc = useTranslations("common");

  const [selectedTopic, setSelectedTopic] = useState<TextualTopic | undefined>(
    undefined
  );
  const [selectedCentury, setSelectedCentury] = useState<number | undefined>(
    undefined
  );
  const [filterYearMin, setFilterYearMin] = useState<number | undefined>(undefined);
  const [filterYearMax, setFilterYearMax] = useState<number | undefined>(undefined);
  const [selectedTradition, setSelectedTradition] = useState<
    string | undefined
  >(undefined);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(1);

  const { data, isLoading } = useTextualStatements({
    topic: selectedTopic,
    century: (filterYearMin || filterYearMax) ? undefined : selectedCentury,
    tradition: selectedTradition,
    yearMin: filterYearMin,
    yearMax: filterYearMax,
    page,
    limit: 20,
  });

  const { data: searchResults } = useSearchStatements(searchQuery);
  const { data: topicsSummary } = useTopicsSummary();

  const displayStatements =
    searchQuery.length >= 2
      ? searchResults ?? []
      : data?.statements ?? [];

  const totalPages = data ? Math.ceil(data.total / 20) : 0;

  return (
    <div className="min-h-screen">
      <Header
        title={t("testimony.title")}
        subtitle={t("testimony.subtitle")}
      />

      <div className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-6">
        <Link
          href="/fathers"
          className="inline-flex items-center gap-2 rounded-lg p-2 text-sm text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("backToList")}
        </Link>

        {/* Filters */}
        <div className="rounded-xl border border-border bg-card p-4 md:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end">
            {/* Topic */}
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("testimony.filterByTopic")}
              </label>
              <select
                value={selectedTopic ?? ""}
                onChange={(e) => {
                  setSelectedTopic(
                    e.target.value === ""
                      ? undefined
                      : (e.target.value as TextualTopic)
                  );
                  setPage(1);
                }}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                <option value="">{t("testimony.allTopics")}</option>
                {ALL_TOPICS.map((topic) => (
                  <option key={topic} value={topic}>
                    {t(`topics.${topic}`)}
                  </option>
                ))}
              </select>
            </div>

            {/* Century */}
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("filterByCentury")}
              </label>
              <select
                value={selectedCentury ?? ""}
                onChange={(e) => {
                  setSelectedCentury(
                    e.target.value === "" ? undefined : Number(e.target.value)
                  );
                  setFilterYearMin(undefined);
                  setFilterYearMax(undefined);
                  setPage(1);
                }}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                <option value="">{t("allCenturies")}</option>
                {Array.from({ length: 10 }, (_, i) => i + 1).map((c) => (
                  <option key={c} value={c}>
                    {tc("century")} {toRoman(c)}
                  </option>
                ))}
              </select>
            </div>

            {/* Year Range */}
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("filterByYear")}
              </label>
              <YearRangeFilter
                yearMin={filterYearMin}
                yearMax={filterYearMax}
                onChange={(min, max) => {
                  setFilterYearMin(min);
                  setFilterYearMax(max);
                  if (min !== undefined || max !== undefined) setSelectedCentury(undefined);
                  setPage(1);
                }}
                disabled={selectedCentury !== undefined}
              />
            </div>

            {/* Tradition */}
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("filterByTradition")}
              </label>
              <select
                value={selectedTradition ?? ""}
                onChange={(e) => {
                  setSelectedTradition(
                    e.target.value === "" ? undefined : e.target.value
                  );
                  setPage(1);
                }}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                <option value="">{t("traditions.all")}</option>
                {["greek", "latin", "syriac", "coptic"].map((tr) => (
                  <option key={tr} value={tr}>
                    {t(`traditions.${tr}`)}
                  </option>
                ))}
              </select>
            </div>

            {/* Keyword */}
            <div className="min-w-0 flex-1">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("testimony.searchKeyword")}
              </label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder={t("testimony.searchPlaceholder")}
                  className="w-full rounded-lg border border-border bg-background py-2 pl-9 pr-3 text-sm"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-6 lg:flex-row">
          {/* Topics Summary Sidebar */}
          {topicsSummary && topicsSummary.topics.length > 0 && (
            <div className="w-full shrink-0 lg:w-64">
              <div className="rounded-xl border border-border bg-card p-4">
                <h3 className="mb-3 text-sm font-semibold">
                  {t("testimony.topicsSummary")}
                </h3>
                <div className="space-y-2">
                  {topicsSummary.topics.map((ts) => (
                    <button
                      key={ts.topic}
                      onClick={() => {
                        setSelectedTopic(
                          selectedTopic === ts.topic
                            ? undefined
                            : (ts.topic as TextualTopic)
                        );
                        setSearchQuery("");
                        setPage(1);
                      }}
                      className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-sm transition-colors ${
                        selectedTopic === ts.topic
                          ? "bg-primary/10 text-primary"
                          : "hover:bg-secondary"
                      }`}
                    >
                      <span>{t(`topics.${ts.topic}`)}</span>
                      <span className="rounded-full bg-secondary px-2 py-0.5 text-xs font-medium">
                        {ts.count}
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Statements List */}
          <div className="min-w-0 flex-1 space-y-4">
            {isLoading && (
              <div className="space-y-4">
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="animate-pulse rounded-xl border border-border bg-card p-6"
                  >
                    <div className="mb-3 h-5 w-32 rounded bg-secondary" />
                    <div className="space-y-2">
                      <div className="h-4 w-full rounded bg-secondary" />
                      <div className="h-4 w-3/4 rounded bg-secondary" />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {!isLoading && displayStatements.length === 0 && (
              <div className="rounded-xl border border-border bg-card p-6 text-center text-muted-foreground">
                {t("statements.noStatements")}
              </div>
            )}

            {displayStatements.map((s) => (
              <div
                key={s.id}
                className="rounded-xl border border-border bg-card p-4 md:p-6"
              >
                <div className="mb-3 flex flex-wrap items-center gap-2">
                  <span
                    className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${
                      TOPIC_COLORS[s.topic] ?? "bg-gray-500/15 text-gray-400"
                    }`}
                  >
                    {t(`topics.${s.topic}`)}
                  </span>
                  <Link
                    href={`/fathers/${s.fatherId}`}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {s.fatherName}
                  </Link>
                  {s.approximateYear && (
                    <span className="text-xs text-muted-foreground">
                      c. {s.approximateYear}
                    </span>
                  )}
                </div>
                <blockquote className="border-l-2 border-primary/30 pl-4 leading-relaxed text-foreground/90 italic">
                  &ldquo;{s.statementText}&rdquo;
                </blockquote>
                {(s.sourceWork || s.sourceReference) && (
                  <p className="mt-3 text-sm text-muted-foreground">
                    {s.sourceWork}
                    {s.sourceReference && ` — ${s.sourceReference}`}
                  </p>
                )}
              </div>
            ))}

            {/* Pagination */}
            {searchQuery.length < 2 && data && data.total > 20 && (
              <div className="flex items-center justify-between pt-2">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page === 1}
                  className="rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium text-secondary-foreground disabled:opacity-50"
                >
                  {tc("previous")}
                </button>
                <span className="text-sm text-muted-foreground">
                  {page} / {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages}
                  className="rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium text-secondary-foreground disabled:opacity-50"
                >
                  {tc("next")}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
