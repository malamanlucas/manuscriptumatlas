"use client";

import { useState, useMemo } from "react";
import { useParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useChurchFatherDetail, useChurchFathers, useFatherCouncils } from "@/hooks/useChurchFathers";
import { useFatherStatements } from "@/hooks/useTextualStatements";
import { Link } from "@/i18n/navigation";
import { toRoman } from "@/lib/utils";
import { ArrowLeft, BookOpen, ChevronDown, Landmark, Quote, Skull } from "lucide-react";
import { DatingBadge } from "@/components/ui/DatingBadge";
import { CouncilTypeBadge } from "@/components/councils/CouncilTypeBadge";
import type { TextualTopic } from "@/types";

const TRADITION_BADGE_COLORS: Record<string, string> = {
  greek: "bg-[#4a6fa5]/20 text-[#4a6fa5] border-[#4a6fa5]/30",
  latin: "bg-[#b8976a]/20 text-[#b8976a] border-[#b8976a]/30",
  syriac: "bg-[#5a8a7a]/20 text-[#5a8a7a] border-[#5a8a7a]/30",
  coptic: "bg-[#7a6e8a]/20 text-[#7a6e8a] border-[#7a6e8a]/30",
};

const TRADITION_DOT_COLORS: Record<string, string> = {
  greek: "bg-[#4a6fa5]",
  latin: "bg-[#b8976a]",
  syriac: "bg-[#5a8a7a]",
  coptic: "bg-[#7a6e8a]",
};

const TOPIC_COLORS: Record<string, string> = {
  MANUSCRIPTS: "bg-[#4a6fa5]/15 text-[#4a6fa5] border-[#4a6fa5]/30",
  AUTOGRAPHS: "bg-[#b8976a]/15 text-[#b8976a] border-[#b8976a]/30",
  APOCRYPHA: "bg-[#a65d57]/15 text-[#a65d57] border-[#a65d57]/30",
  CANON: "bg-[#5a8a7a]/15 text-[#5a8a7a] border-[#5a8a7a]/30",
  TEXTUAL_VARIANTS: "bg-[#7a6e8a]/15 text-[#7a6e8a] border-[#7a6e8a]/30",
  TRANSLATION: "bg-[#5e8e9e]/15 text-[#5e8e9e] border-[#5e8e9e]/30",
  CORRUPTION: "bg-[#9e7b5a]/15 text-[#9e7b5a] border-[#9e7b5a]/30",
  SCRIPTURE_AUTHORITY: "bg-[#6a7a9e]/15 text-[#6a7a9e] border-[#6a7a9e]/30",
};

export default function FatherDetailPage() {
  const params = useParams();
  const id = Number(params.id as string);
  const t = useTranslations("fathers");
  const tc = useTranslations("common");

  const [activeTab, setActiveTab] = useState<"info" | "statements" | "councils">("info");
  const [topicFilter, setTopicFilter] = useState<TextualTopic | "all">("all");
  const [bioExpanded, setBioExpanded] = useState(false);

  const { data: father, isLoading, error } = useChurchFatherDetail(
    isNaN(id) ? null : id
  );
  const { data: allFathers } = useChurchFathers({ limit: 100 });
  const { data: statements, isLoading: statementsLoading } = useFatherStatements(
    isNaN(id) ? null : id
  );
  const { data: fatherCouncils, isLoading: councilsLoading } = useFatherCouncils(
    isNaN(id) ? null : id
  );

  const contemporaries = useMemo(() => {
    if (!father || !allFathers?.fathers) return [];
    return allFathers.fathers.filter(
      (f) =>
        f.id !== father.id &&
        f.centuryMin <= father.centuryMax &&
        f.centuryMax >= father.centuryMin
    );
  }, [father, allFathers]);

  const filteredStatements = useMemo(() => {
    if (!statements) return [];
    if (topicFilter === "all") return statements;
    return statements.filter((s) => s.topic === topicFilter);
  }, [statements, topicFilter]);

  const statementTopics = useMemo(() => {
    if (!statements) return [];
    return [...new Set(statements.map((s) => s.topic))];
  }, [statements]);

  if (isLoading) {
    return (
      <div className="min-h-screen">
        <Header title={t("detail")} subtitle="" />
        <div className="space-y-4 p-4 md:p-6">
          <div className="animate-pulse rounded-xl border border-border bg-card p-6">
            <div className="mb-4 h-6 w-48 rounded bg-secondary" />
            <div className="h-4 w-96 rounded bg-secondary" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !father) {
    return (
      <div className="min-h-screen">
        <Header title={tc("error")} subtitle="" />
        <div className="p-4 md:p-6">
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {error ? (error as Error).message : "Church father not found"}
          </div>
        </div>
      </div>
    );
  }

  const centuryLabel = `${toRoman(father.centuryMin)}${
    father.centuryMax !== father.centuryMin ? `–${toRoman(father.centuryMax)}` : ""
  }`;

  return (
    <div className="min-h-screen">
      <Header title={father.displayName} subtitle={`${tc("century")} ${centuryLabel}`} />

      <div className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-6">
        <Link
          href="/fathers"
          className="inline-flex items-center gap-2 rounded-lg p-2 text-sm text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("backToList")}
        </Link>

        {/* Tabs */}
        <div className="flex gap-1 overflow-x-auto rounded-lg border border-border bg-card p-1">
          <button
            onClick={() => setActiveTab("info")}
            className={`flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === "info"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-secondary hover:text-foreground"
            }`}
          >
            <BookOpen className="h-4 w-4" />
            {t("tabs.info")}
          </button>
          <button
            onClick={() => setActiveTab("statements")}
            className={`flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === "statements"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-secondary hover:text-foreground"
            }`}
          >
            <Quote className="h-4 w-4" />
            {t("tabs.statements")}
            {statements && statements.length > 0 && (
              <span className="ml-1 rounded-full bg-white/20 px-1.5 py-0.5 text-xs">
                {statements.length}
              </span>
            )}
          </button>
          <button
            onClick={() => setActiveTab("councils")}
            className={`flex items-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === "councils"
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-secondary hover:text-foreground"
            }`}
          >
            <Landmark className="h-4 w-4" />
            {t("tabs.councils")}
            {fatherCouncils && fatherCouncils.length > 0 && (
              <span className="ml-1 rounded-full bg-white/20 px-1.5 py-0.5 text-xs">
                {fatherCouncils.length}
              </span>
            )}
          </button>
        </div>

        {activeTab === "info" && (
          <>
            {/* Main Info */}
            <div className="rounded-xl border border-border bg-card p-4 md:p-6">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
                <div>
                  <p className="text-xs font-medium text-muted-foreground">
                    {t("centuries")}
                  </p>
                  <p className="mt-1 text-lg font-semibold">{centuryLabel}</p>
                </div>
                {father && (father.yearMin || father.yearMax || father.yearBest) && (
                  <div>
                    <p className="text-xs text-muted-foreground">{t("dating")}</p>
                    <DatingBadge
                      yearMin={father.yearMin}
                      yearMax={father.yearMax}
                      yearBest={father.yearBest}
                      datingSource={father.datingSource}
                      datingConfidence={father.datingConfidence}
                      datingReference={father.datingReference}
                    />
                  </div>
                )}
                <div>
                  <p className="text-xs font-medium text-muted-foreground">
                    {t("tradition")}
                  </p>
                  <div className="mt-1">
                    <span
                      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-sm font-medium ${
                        TRADITION_BADGE_COLORS[father.tradition] ?? ""
                      }`}
                    >
                      {t(`traditions.${father.tradition}`)}
                    </span>
                  </div>
                </div>
                <div>
                  <p className="text-xs font-medium text-muted-foreground">
                    {t("location")}
                  </p>
                  <p className="mt-1 text-sm">{father.primaryLocation ?? "—"}</p>
                </div>
                {father.mannerOfDeath && (
                  <div>
                    <p className="text-xs font-medium text-muted-foreground">
                      <span className="inline-flex items-center gap-1">
                        <Skull className="h-3 w-3" />
                        {t("mannerOfDeath")}
                      </span>
                    </p>
                    <p className="mt-1 text-sm">{father.mannerOfDeath}</p>
                  </div>
                )}
                <div>
                  <p className="text-xs font-medium text-muted-foreground">
                    {t("source")}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">{father.source}</p>
                </div>
              </div>
            </div>

            {/* Description */}
            {father.shortDescription && (
              <div className="rounded-xl border border-border bg-card p-4 md:p-6">
                <h2 className="mb-3 text-base font-semibold">{t("description")}</h2>
                <p className="max-w-3xl leading-relaxed text-muted-foreground">
                  {father.shortDescription}
                </p>
              </div>
            )}

            {/* Biography */}
            {father.biographySummary && (
              <div className="rounded-xl border border-border bg-card p-4 md:p-6">
                <h2 className="mb-3 text-base font-semibold">{t("biography")}</h2>
                <p className="max-w-3xl leading-relaxed text-muted-foreground">
                  {father.biographySummary}
                </p>

                {father.biographyIsLong && father.biographyOriginal && (
                  <div className="mt-4">
                    <div className="relative">
                      <div
                        className={`overflow-hidden transition-all duration-300 ease-in-out ${
                          bioExpanded ? "max-h-80 overflow-y-auto" : "max-h-0"
                        }`}
                      >
                        <div className="rounded-lg bg-muted/50 p-4">
                          <p className="max-w-3xl leading-relaxed text-muted-foreground whitespace-pre-line">
                            {father.biographyOriginal}
                          </p>
                        </div>
                      </div>
                      {!bioExpanded && (
                        <div className="pointer-events-none absolute bottom-0 left-0 right-0 h-8 bg-gradient-to-t from-card to-transparent" />
                      )}
                    </div>
                    <button
                      onClick={() => setBioExpanded(!bioExpanded)}
                      className="mt-2 inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-primary transition-colors hover:bg-primary/10"
                    >
                      {bioExpanded ? t("collapseFullBiography") : t("readFullBiography")}
                      <ChevronDown
                        className={`h-4 w-4 transition-transform duration-300 ${
                          bioExpanded ? "rotate-180" : ""
                        }`}
                      />
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Contemporaries */}
            {contemporaries.length > 0 && (
              <div className="rounded-xl border border-border bg-card p-4 md:p-6">
                <h2 className="mb-1 text-base font-semibold">
                  {t("contemporariesOf", { name: father.displayName })}
                </h2>
                <p className="mb-4 text-sm text-muted-foreground">
                  {t("contemporariesDescription", { century: centuryLabel })}
                </p>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {contemporaries.map((f) => (
                    <Link
                      key={f.id}
                      href={`/fathers/${f.id}`}
                      className="group flex items-center gap-3 rounded-lg border border-border bg-background p-3 transition-all hover:border-primary/30 hover:shadow-md"
                    >
                      <span
                        className={`h-3 w-3 shrink-0 rounded-full ${
                          TRADITION_DOT_COLORS[f.tradition] ?? "bg-gray-500"
                        }`}
                      />
                      <div className="min-w-0">
                        <p className="truncate font-medium group-hover:text-primary">
                          {f.displayName}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {toRoman(f.centuryMin)}
                          {f.centuryMax !== f.centuryMin && `–${toRoman(f.centuryMax)}`}
                          {f.primaryLocation && ` · ${f.primaryLocation}`}
                        </p>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        {activeTab === "statements" && (
          <>
            {/* Topic filter */}
            {statementTopics.length > 1 && (
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setTopicFilter("all")}
                  className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                    topicFilter === "all"
                      ? "bg-primary text-primary-foreground"
                      : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
                >
                  {t("traditions.all")}
                </button>
                {statementTopics.map((topic) => (
                  <button
                    key={topic}
                    onClick={() => setTopicFilter(topic as TextualTopic)}
                    className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                      topicFilter === topic
                        ? "bg-primary text-primary-foreground"
                        : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                    }`}
                  >
                    {t(`topics.${topic}`)}
                  </button>
                ))}
              </div>
            )}

            {statementsLoading && (
              <div className="space-y-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="animate-pulse rounded-xl border border-border bg-card p-6">
                    <div className="mb-3 h-5 w-24 rounded bg-secondary" />
                    <div className="space-y-2">
                      <div className="h-4 w-full rounded bg-secondary" />
                      <div className="h-4 w-3/4 rounded bg-secondary" />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {!statementsLoading && filteredStatements.length === 0 && (
              <div className="rounded-xl border border-border bg-card p-6 text-center text-muted-foreground">
                {t("statements.noStatements")}
              </div>
            )}

            {filteredStatements.map((s) => (
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
          </>
        )}

        {activeTab === "councils" && (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            <h2 className="mb-1 text-base font-semibold">{t("councilsTitle")}</h2>
            <p className="mb-4 text-sm text-muted-foreground">{t("councilsDescription")}</p>

            {councilsLoading && (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="animate-pulse rounded-lg border border-border p-4">
                    <div className="h-4 w-52 rounded bg-secondary" />
                    <div className="mt-2 h-3 w-36 rounded bg-secondary" />
                  </div>
                ))}
              </div>
            )}

            {!councilsLoading && (!fatherCouncils || fatherCouncils.length === 0) && (
              <p className="text-sm text-muted-foreground">{t("noCouncils")}</p>
            )}

            {!councilsLoading && !!fatherCouncils?.length && (
              <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
                {fatherCouncils.map((council) => (
                  <Link
                    key={council.id}
                    href={`/councils/${council.slug}`}
                    className="rounded-lg border border-border p-4 transition-colors hover:bg-secondary/30"
                  >
                    <p className="font-medium">{council.displayName}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {council.year}
                      {council.yearEnd ? `-${council.yearEnd}` : ""}
                      {council.location ? ` · ${council.location}` : ""}
                    </p>
                    <div className="mt-2 flex items-center gap-2">
                      <CouncilTypeBadge type={council.councilType} />
                      <span className="text-xs text-muted-foreground">
                        {(council.consensusConfidence * 100).toFixed(0)}%
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
