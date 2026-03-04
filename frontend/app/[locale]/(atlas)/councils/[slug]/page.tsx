"use client";

import { useState, type ReactNode } from "react";
import { useParams } from "next/navigation";
import dynamic from "next/dynamic";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { Link } from "@/i18n/navigation";
import { useCouncilDetail, useCouncilCanons } from "@/hooks/useCouncils";
import { CouncilTypeBadge } from "@/components/councils/CouncilTypeBadge";
import { SummaryToggle } from "@/components/councils/SummaryToggle";
import { SourceProvenancePanel } from "@/components/councils/SourceProvenancePanel";
import { ConfidenceDot } from "@/components/ui/ConfidenceDot";
import { ArrowLeft, BookOpen, ScrollText, Users, ShieldAlert, Database, ChevronDown, ChevronUp, ExternalLink, Info } from "lucide-react";

const CouncilMapView = dynamic(
  () => import("@/components/councils/CouncilMapView").then((m) => m.CouncilMapView),
  { ssr: false }
);

type Tab = "overview" | "canons" | "participants" | "heresies" | "sources";

export default function CouncilDetailPage() {
  const params = useParams();
  const slug = String(params.slug ?? "");
  const t = useTranslations("councils");
  const tc = useTranslations("common");
  const [tab, setTab] = useState<Tab>("overview");
  const [showConflict, setShowConflict] = useState(false);
  const [showMethodology, setShowMethodology] = useState(false);

  const detailQuery = useCouncilDetail(slug);
  const canonsQuery = useCouncilCanons(slug, 1, 200);

  if (detailQuery.isLoading) {
    return (
      <div className="min-h-screen">
        <Header title={t("detailTitle")} subtitle={tc("loading")} />
      </div>
    );
  }

  if (detailQuery.error || !detailQuery.data) {
    return (
      <div className="min-h-screen">
        <Header title={tc("error")} subtitle="" />
        <div className="p-4 md:p-6 text-red-500">{(detailQuery.error as Error)?.message ?? t("notFound")}</div>
      </div>
    );
  }

  const council = detailQuery.data;

  const mapPoint =
    council.latitude != null && council.longitude != null
      ? [
          {
            id: council.id,
            slug: council.slug,
            displayName: council.displayName,
            year: council.year,
            councilType: council.councilType,
            latitude: council.latitude,
            longitude: council.longitude,
          },
        ]
      : [];

  return (
    <div className="min-h-screen">
      <Header title={council.displayName} subtitle={`${t("year")}: ${council.year}${council.yearEnd ? `-${council.yearEnd}` : ""}`} />

      <div className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-6">
        <Link href="/councils" className="inline-flex items-center gap-2 rounded-lg p-2 text-sm text-muted-foreground hover:bg-secondary">
          <ArrowLeft className="h-4 w-4" />
          {t("backToList")}
        </Link>

        <div className="grid gap-4 lg:grid-cols-[1fr_300px]">
          <div className="space-y-4 rounded-xl border border-border bg-card p-4 md:p-6">
            <div className="flex flex-wrap items-center gap-2">
              <CouncilTypeBadge type={council.councilType} />
              <span className="text-sm text-muted-foreground">{council.location ?? "—"}</span>
              <span className="inline-flex items-center gap-1 text-sm text-muted-foreground">
                <ConfidenceDot confidence={council.dataConfidence} source="consensus" />
                {(council.consensusConfidence * 100).toFixed(0)}%
              </span>
            </div>

            <div className="flex flex-wrap gap-1 rounded-lg border border-border bg-background p-1">
              <TabButton active={tab === "overview"} onClick={() => setTab("overview")} icon={<BookOpen className="h-4 w-4" />} label={t("tabs.overview")} />
              <TabButton active={tab === "canons"} onClick={() => setTab("canons")} icon={<ScrollText className="h-4 w-4" />} label={t("tabs.canons")} />
              <TabButton active={tab === "participants"} onClick={() => setTab("participants")} icon={<Users className="h-4 w-4" />} label={t("tabs.participants")} />
              <TabButton active={tab === "heresies"} onClick={() => setTab("heresies")} icon={<ShieldAlert className="h-4 w-4" />} label={t("tabs.heresies")} />
              <TabButton active={tab === "sources"} onClick={() => setTab("sources")} icon={<Database className="h-4 w-4" />} label={t("tabs.sources")} />
            </div>

            {tab === "overview" && (
              <div className="space-y-4">
                <SummaryToggle summary={council.summary} originalText={council.originalText} />
                {council.mainTopics && (
                  <p className="text-sm text-muted-foreground">{council.mainTopics}</p>
                )}
              </div>
            )}

            {tab === "canons" && (
              <div className="space-y-3">
                {(canonsQuery.data ?? []).map((canon) => (
                  <div key={canon.id} className="rounded-lg border border-border p-3">
                    <p className="text-sm font-semibold">{t("canon")} {canon.canonNumber}{canon.title ? ` - ${canon.title}` : ""}</p>
                    <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">{canon.canonText}</p>
                  </div>
                ))}
                {(canonsQuery.data ?? []).length === 0 && (
                  <p className="text-sm text-muted-foreground">{t("noCanons")}</p>
                )}
              </div>
            )}

            {tab === "participants" && (
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {council.relatedFathers.map((father) => (
                  <Link key={father.fatherId} href={`/fathers/${father.fatherId}`} className="rounded-lg border border-border p-3 text-sm hover:bg-secondary/40">
                    <p className="font-medium">{father.fatherName}</p>
                    {father.role && <p className="text-xs text-muted-foreground">{father.role}</p>}
                  </Link>
                ))}
                {council.relatedFathers.length === 0 && (
                  <p className="text-sm text-muted-foreground">{t("noParticipants")}</p>
                )}
              </div>
            )}

            {tab === "heresies" && (
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {council.heresies.map((heresy) => (
                  <Link key={heresy.id} href={`/heresies/${heresy.slug}`} className="rounded-lg border border-border p-3 text-sm hover:bg-secondary/40">
                    <p className="font-medium">{heresy.name}</p>
                    {heresy.keyFigure && <p className="text-xs text-muted-foreground">{heresy.keyFigure}</p>}
                  </Link>
                ))}
                {council.heresies.length === 0 && (
                  <p className="text-sm text-muted-foreground">{t("noHeresies")}</p>
                )}
              </div>
            )}

            {tab === "sources" && <SourceProvenancePanel claims={council.sourceClaims} />}
          </div>

          <aside className="space-y-4">
            <div className="rounded-xl border border-border bg-card p-4">
              <h3 className="text-sm font-semibold">{t("quickInfo")}</h3>
              <div className="mt-3 space-y-2 text-sm">
                <p><span className="text-muted-foreground">{t("year")}:</span> {council.year}{council.yearEnd ? `-${council.yearEnd}` : ""}</p>
                <p><span className="text-muted-foreground">{tc("type")}:</span> {council.councilType}</p>
                <p><span className="text-muted-foreground">{t("participants")}:</span> {council.numberOfParticipants ?? "—"}</p>
                <p><span className="text-muted-foreground">{t("sources")}:</span> {council.sourceCount}</p>
              </div>
            </div>

            <ConfidenceCard
              confidence={council.consensusConfidence}
              dataConfidence={council.dataConfidence}
              sourceCount={council.sourceCount}
              conflictResolution={council.conflictResolution}
              sourceClaims={council.sourceClaims}
              showConflict={showConflict}
              onToggleConflict={() => setShowConflict((v) => !v)}
              showMethodology={showMethodology}
              onToggleMethodology={() => setShowMethodology((v) => !v)}
            />

            {mapPoint.length > 0 && <CouncilMapView points={mapPoint} height={260} />}
          </aside>
        </div>
      </div>
    </div>
  );
}

function TabButton({
  active,
  onClick,
  icon,
  label,
}: {
  active: boolean;
  onClick: () => void;
  icon: ReactNode;
  label: string;
}) {
  return (
    <button
      onClick={onClick}
      className={`inline-flex items-center gap-1.5 rounded-md px-3 py-2 text-xs font-medium ${active ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-secondary"}`}
    >
      {icon}
      {label}
    </button>
  );
}

const confidenceBarColor: Record<string, string> = {
  HIGH: "bg-emerald-500",
  MEDIUM: "bg-amber-500",
  LOW: "bg-red-500",
};

const sourceLevelColors: Record<string, string> = {
  PRIMARY: "bg-emerald-500/15 text-emerald-400",
  ACADEMIC: "bg-blue-500/15 text-blue-400",
  STRUCTURED: "bg-violet-500/15 text-violet-400",
  AGGREGATOR: "bg-zinc-500/15 text-zinc-400",
};

function ConfidenceCard({
  confidence,
  dataConfidence,
  sourceCount,
  conflictResolution,
  sourceClaims,
  showConflict,
  onToggleConflict,
  showMethodology,
  onToggleMethodology,
}: {
  confidence: number;
  dataConfidence: string;
  sourceCount: number;
  conflictResolution?: string | null;
  sourceClaims: { sourceDisplayName: string; sourceLevel: string; sourcePage?: string | null }[];
  showConflict: boolean;
  onToggleConflict: () => void;
  showMethodology: boolean;
  onToggleMethodology: () => void;
}) {
  const t = useTranslations("councils");
  const pct = (confidence * 100).toFixed(0);
  const level = dataConfidence.toUpperCase();
  const barColor = confidenceBarColor[level] ?? "bg-zinc-500";
  const levelKey = level === "HIGH" ? "confidenceHigh" : level === "MEDIUM" ? "confidenceMedium" : "confidenceLow";

  const uniqueSources = sourceClaims.reduce<{ name: string; level: string; url?: string | null }[]>((acc, claim) => {
    if (!acc.some((s) => s.name === claim.sourceDisplayName)) {
      acc.push({ name: claim.sourceDisplayName, level: claim.sourceLevel, url: claim.sourcePage });
    }
    return acc;
  }, []);

  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <h3 className="text-sm font-semibold">{t("confidenceLabel")}</h3>

      <div className="mt-3 space-y-3">
        <div>
          <div className="mb-1 flex items-center justify-between text-sm">
            <span className="inline-flex items-center gap-1.5">
              <span className={`inline-block h-2 w-2 rounded-full ${barColor}`} />
              {pct}%
            </span>
            <span className="text-xs text-muted-foreground">{t("sourceCountShort", { count: sourceCount })}</span>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
            <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${pct}%` }} />
          </div>
        </div>

        <p className="text-xs leading-relaxed text-muted-foreground">{t(levelKey)}</p>

        {conflictResolution && (
          <div>
            <button
              onClick={onToggleConflict}
              className="inline-flex w-full items-center justify-between rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-1.5 text-xs font-medium text-amber-400"
            >
              {t("conflictResolved")}
              {showConflict ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            </button>
            {showConflict && (
              <div className="mt-2 rounded-lg bg-muted/40 px-3 py-2 text-xs leading-relaxed text-muted-foreground">
                <p className="mb-1 font-medium">{t("conflictDetail")}</p>
                <p>{conflictResolution}</p>
              </div>
            )}
          </div>
        )}

        {uniqueSources.length > 0 && (
          <div>
            <p className="mb-2 text-xs font-medium text-muted-foreground">{t("sourcesUsed")}</p>
            <div className="space-y-1.5">
              {uniqueSources.map((source) => (
                <div key={source.name} className="flex items-start gap-2 text-xs">
                  <span className={`mt-0.5 shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase ${sourceLevelColors[source.level] ?? "bg-muted text-muted-foreground"}`}>
                    {source.level}
                  </span>
                  <span className="min-w-0">
                    {source.url?.startsWith("http://") || source.url?.startsWith("https://") ? (
                      <a href={source.url} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 hover:text-primary hover:underline">
                        <span className="truncate">{source.name}</span>
                        <ExternalLink className="h-2.5 w-2.5 shrink-0 opacity-60" />
                      </a>
                    ) : (
                      <Link href="/sources#councilSources" className="inline-flex items-center gap-1 hover:text-primary hover:underline">
                        <span className="truncate">{source.name}</span>
                        <Info className="h-2.5 w-2.5 shrink-0 opacity-60" />
                      </Link>
                    )}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        <button
          onClick={onToggleMethodology}
          className="inline-flex w-full items-center gap-1 text-[11px] text-muted-foreground/70 hover:text-muted-foreground"
        >
          {showMethodology ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
          {t("methodology")}
        </button>
        {showMethodology && (
          <div className="rounded-lg bg-muted/40 px-3 py-2 text-[11px] leading-relaxed text-muted-foreground">
            <p>{t("methodologyText")}</p>
            <p className="mt-1 font-medium">{t("confidenceLevels")}</p>
          </div>
        )}
      </div>
    </div>
  );
}
