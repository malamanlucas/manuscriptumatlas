"use client";

import { useMemo, useState } from "react";
import dynamic from "next/dynamic";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { CenturyRangeFilter } from "@/components/filters/CenturyRangeFilter";
import { YearRangeFilter } from "@/components/filters/YearRangeFilter";
import { useCouncils, useSearchCouncils, useCouncilMapPoints } from "@/hooks/useCouncils";
import { CouncilsTimelineChart } from "@/components/charts/CouncilsTimelineChart";
import { CouncilTypeBadge } from "@/components/councils/CouncilTypeBadge";
import { ConfidenceDot } from "@/components/ui/ConfidenceDot";
import { Link } from "@/i18n/navigation";
import { Search, Map, Table2, Info } from "lucide-react";

const CouncilMapView = dynamic(
  () => import("@/components/councils/CouncilMapView").then((m) => m.CouncilMapView),
  { ssr: false }
);

export default function CouncilsPage() {
  const t = useTranslations("councils");
  const tc = useTranslations("common");

  const [selectedCentury, setSelectedCentury] = useState<number | undefined>();
  const [type, setType] = useState<string>("all");
  const [search, setSearch] = useState("");
  const [yearMin, setYearMin] = useState<number | undefined>();
  const [yearMax, setYearMax] = useState<number | undefined>();
  const [page, setPage] = useState(1);
  const [mapMode, setMapMode] = useState(false);
  const [showConfidenceHelp, setShowConfidenceHelp] = useState(false);

  const listQuery = useCouncils({
    century: yearMin || yearMax ? undefined : selectedCentury,
    type: type === "all" ? undefined : type,
    yearMin,
    yearMax,
    page,
    limit: 25,
  });
  const searchQuery = useSearchCouncils(search, 50);
  const mapQuery = useCouncilMapPoints();

  const councils = useMemo(() => {
    if (search.trim().length >= 2) return searchQuery.data ?? [];
    return listQuery.data?.councils ?? [];
  }, [search, searchQuery.data, listQuery.data]);

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-6">
        <div className="space-y-4 rounded-xl border border-border bg-card p-4 md:p-6">
          <div>
            <label className="mb-2 block text-xs font-medium text-muted-foreground">{t("filterByCentury")}</label>
            <CenturyRangeFilter
              value={selectedCentury}
              onChange={(c) => {
                setSelectedCentury(c);
                if (c !== undefined) {
                  setYearMin(undefined);
                  setYearMax(undefined);
                }
                setPage(1);
              }}
            />
          </div>

          <div>
            <label className="mb-2 block text-xs font-medium text-muted-foreground">{t("filterByYear")}</label>
            <YearRangeFilter
              yearMin={yearMin}
              yearMax={yearMax}
              onChange={(min, max) => {
                setYearMin(min);
                setYearMax(max);
                if (min !== undefined || max !== undefined) setSelectedCentury(undefined);
                setPage(1);
              }}
              disabled={selectedCentury !== undefined}
            />
          </div>

          <div className="flex flex-col gap-3 md:flex-row md:items-end">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{tc("type")}</label>
              <div className="flex flex-wrap gap-2">
                {["all", "ECUMENICAL", "REGIONAL", "LOCAL"].map((entry) => (
                  <button
                    key={entry}
                    onClick={() => {
                      setType(entry);
                      setPage(1);
                    }}
                    className={`rounded-lg px-3 py-2 text-sm font-medium ${type === entry ? "bg-primary text-primary-foreground" : "bg-secondary text-secondary-foreground hover:bg-secondary/80"}`}
                  >
                    {entry === "all" ? tc("all") : entry}
                  </button>
                ))}
              </div>
            </div>

            <div className="min-w-0 md:ml-auto md:w-72">
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{tc("search")}</label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder={t("searchPlaceholder")}
                  className="w-full rounded-lg border border-border bg-background py-2 pl-9 pr-3 text-sm"
                />
              </div>
            </div>

            <button
              onClick={() => setMapMode((v) => !v)}
              className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-sm font-medium"
            >
              {mapMode ? <Table2 className="h-4 w-4" /> : <Map className="h-4 w-4" />}
              {mapMode ? t("tableView") : t("mapView")}
            </button>
          </div>
        </div>

        {!mapMode && councils.length > 0 && (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            <h2 className="mb-4 text-base font-semibold">{t("timelineTitle")}</h2>
            <CouncilsTimelineChart councils={councils} onCenturyClick={(c) => setSelectedCentury(c)} />
          </div>
        )}

        {mapMode ? (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            <CouncilMapView points={mapQuery.data ?? []} />
          </div>
        ) : (
          <div className="rounded-xl border border-border bg-card p-4 md:p-6">
            {listQuery.isLoading && <div className="text-sm text-muted-foreground">{tc("loading")}</div>}
            {listQuery.error && <div className="text-sm text-red-500">{(listQuery.error as Error).message}</div>}
            {!listQuery.isLoading && councils.length === 0 && (
              <div className="text-sm text-muted-foreground">{t("noResults")}</div>
            )}

            {councils.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border text-left text-xs uppercase text-muted-foreground">
                      <th className="py-2">{tc("name")}</th>
                      <th className="py-2">{t("year")}</th>
                      <th className="py-2">{tc("type")}</th>
                      <th className="py-2">{t("participants")}</th>
                      <th className="py-2">
                        <span className="inline-flex items-center gap-1">
                          {t("confidence")}
                          <button
                            onClick={() => setShowConfidenceHelp((v) => !v)}
                            className="text-muted-foreground/70 hover:text-foreground"
                            aria-label={t("methodology")}
                          >
                            <Info className="h-3 w-3" />
                          </button>
                        </span>
                      </th>
                    </tr>
                    {showConfidenceHelp && (
                      <tr>
                        <td colSpan={5} className="pb-3 pt-1">
                          <div className="rounded-lg border border-border bg-muted/40 px-4 py-3 text-xs leading-relaxed text-muted-foreground">
                            <p>{t("confidenceExplainer")}</p>
                            <p className="mt-1 font-medium">{t("confidenceLevels")}</p>
                            <p className="mt-1 text-[11px] opacity-80">{t("methodologyText")}</p>
                          </div>
                        </td>
                      </tr>
                    )}
                  </thead>
                  <tbody>
                    {councils.map((c) => (
                      <tr key={c.id} className="border-b border-border/50 hover:bg-secondary/30">
                        <td className="py-2">
                          <Link href={`/councils/${c.slug}`} className="font-medium hover:text-primary hover:underline">
                            {c.displayName}
                          </Link>
                          {c.location && <span className="ml-2 text-xs text-muted-foreground">· {c.location}</span>}
                        </td>
                        <td className="py-2">
                          {c.year}
                          {c.yearEnd ? `-${c.yearEnd}` : ""}
                        </td>
                        <td className="py-2">
                          <CouncilTypeBadge type={c.councilType} />
                        </td>
                        <td className="py-2">{c.numberOfParticipants ?? "—"}</td>
                        <td className="py-2">
                          <span className="inline-flex items-center gap-2">
                            <ConfidenceDot confidence={c.dataConfidence} source={`score=${c.consensusConfidence.toFixed(2)}`} />
                            <span>{(c.consensusConfidence * 100).toFixed(0)}%</span>
                            <span className="text-xs text-muted-foreground">· {t("sourceCountShort", { count: c.sourceCount })}</span>
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {listQuery.data && search.trim().length < 2 && listQuery.data.total > 25 && (
              <div className="mt-4 flex items-center justify-between">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page === 1}
                  className="rounded-lg bg-secondary px-3 py-1.5 text-sm disabled:opacity-50"
                >
                  {tc("previous")}
                </button>
                <span className="text-sm text-muted-foreground">
                  {page} / {Math.ceil(listQuery.data.total / 25)}
                </span>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page * 25 >= listQuery.data.total}
                  className="rounded-lg bg-secondary px-3 py-1.5 text-sm disabled:opacity-50"
                >
                  {tc("next")}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
