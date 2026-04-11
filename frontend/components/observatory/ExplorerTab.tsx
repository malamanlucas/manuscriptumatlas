"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  useAnalyticsSessions,
  useAnalyticsFilterValues,
} from "@/hooks/useVisitorAnalytics";
import { SessionDrawer } from "./SessionDrawer";
import type { SessionFilters, VisitorSessionCompact } from "@/types";
import { ChevronLeft, ChevronRight, Maximize2, Minimize2 } from "lucide-react";

export function ExplorerTab({ filters }: { filters: SessionFilters }) {
  const t = useTranslations("observatory");
  const [view, setView] = useState<"compact" | "complete">("compact");
  const [page, setPage] = useState(1);
  const [selectedSession, setSelectedSession] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  const [filterBrowser, setFilterBrowser] = useState<string>();
  const [filterOs, setFilterOs] = useState<string>();
  const [filterDevice, setFilterDevice] = useState<string>();

  const { data: filterValues } = useAnalyticsFilterValues();

  const queryFilters: SessionFilters = {
    ...filters,
    view,
    page,
    limit: 25,
    browser: filterBrowser,
    os: filterOs,
    deviceType: filterDevice,
    ip: searchQuery.includes(".") ? searchQuery : undefined,
    visitorId:
      searchQuery.length === 36 && searchQuery.includes("-")
        ? searchQuery
        : undefined,
    fingerprint:
      searchQuery.length > 10 &&
      !searchQuery.includes(".") &&
      !searchQuery.includes("-")
        ? searchQuery
        : undefined,
  };

  const { data, isLoading } = useAnalyticsSessions(queryFilters);
  const sessions = (data?.sessions ?? []) as VisitorSessionCompact[];
  const total = data?.total ?? 0;
  const totalPages = Math.ceil(total / 25);

  return (
    <div className="space-y-4">
      {/* Search + View toggle */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <input
          type="text"
          placeholder={t("explorer.searchPlaceholder")}
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            setPage(1);
          }}
          className="rounded-lg border border-border bg-card px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary sm:w-80"
        />
        <button
          onClick={() => setView(view === "compact" ? "complete" : "compact")}
          className="flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-xs font-medium hover:bg-muted transition-colors"
        >
          {view === "compact" ? (
            <>
              <Maximize2 className="h-3.5 w-3.5" /> {t("explorer.complete")}
            </>
          ) : (
            <>
              <Minimize2 className="h-3.5 w-3.5" /> {t("explorer.compact")}
            </>
          )}
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-2">
        <FilterSelect
          label={t("table.browser")}
          value={filterBrowser}
          options={filterValues?.browsers ?? []}
          onChange={(v) => { setFilterBrowser(v); setPage(1); }}
        />
        <FilterSelect
          label="OS"
          value={filterOs}
          options={filterValues?.operatingSystems ?? []}
          onChange={(v) => { setFilterOs(v); setPage(1); }}
        />
        <FilterSelect
          label={t("table.device")}
          value={filterDevice}
          options={filterValues?.deviceTypes ?? []}
          onChange={(v) => { setFilterDevice(v); setPage(1); }}
        />
      </div>

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-border bg-card">
        <table className="w-full text-xs">
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="px-3 py-2.5">{t("table.visitor")}</th>
              <th className="px-3 py-2.5">IP</th>
              <th className="px-3 py-2.5">{t("table.browser")}</th>
              <th className="px-3 py-2.5">OS</th>
              <th className="px-3 py-2.5">{t("table.device")}</th>
              <th className="hidden px-3 py-2.5 md:table-cell">{t("table.language")}</th>
              <th className="hidden px-3 py-2.5 lg:table-cell">{t("table.timezone")}</th>
              <th className="hidden px-3 py-2.5 sm:table-cell">{t("table.loadTime")}</th>
              <th className="px-3 py-2.5">{t("table.created")}</th>
            </tr>
          </thead>
          <tbody>
            {sessions.map((s) => (
              <tr
                key={s.sessionId}
                onClick={() => setSelectedSession(s.sessionId)}
                className="cursor-pointer border-b border-border/50 hover:bg-muted/50 transition-colors last:border-0"
              >
                <td className="px-3 py-2 font-mono">
                  {s.visitorId.slice(0, 8)}...
                </td>
                <td className="px-3 py-2 font-mono">{s.ipAddress}</td>
                <td className="px-3 py-2">
                  {s.browserName ?? "—"}{" "}
                  <span className="text-muted-foreground">
                    {s.browserVersion?.split(".")[0]}
                  </span>
                </td>
                <td className="px-3 py-2">{s.osName ?? "—"}</td>
                <td className="px-3 py-2">{s.deviceType ?? "—"}</td>
                <td className="hidden px-3 py-2 md:table-cell">{s.language ?? "—"}</td>
                <td className="hidden px-3 py-2 truncate max-w-[120px] lg:table-cell">
                  {s.timezone ?? "—"}
                </td>
                <td className="hidden px-3 py-2 sm:table-cell">
                  {s.pageLoadTimeMs ? `${s.pageLoadTimeMs}ms` : "—"}
                </td>
                <td className="px-3 py-2 whitespace-nowrap">
                  {new Date(s.createdAt).toLocaleString([], {
                    month: "short",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </td>
              </tr>
            ))}
            {sessions.length === 0 && !isLoading && (
              <tr>
                <td colSpan={9} className="px-3 py-8 text-center text-muted-foreground">
                  {t("noData")}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>
            {t("explorer.showing")} {(page - 1) * 25 + 1}-
            {Math.min(page * 25, total)} {t("explorer.of")} {total}
          </span>
          <div className="flex items-center gap-2">
            <button
              disabled={page <= 1}
              onClick={() => setPage(page - 1)}
              className="rounded-md border border-border p-1.5 hover:bg-muted disabled:opacity-30"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span>
              {page} / {totalPages}
            </span>
            <button
              disabled={page >= totalPages}
              onClick={() => setPage(page + 1)}
              className="rounded-md border border-border p-1.5 hover:bg-muted disabled:opacity-30"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {/* Session Drawer */}
      <SessionDrawer
        sessionId={selectedSession}
        onClose={() => setSelectedSession(null)}
      />
    </div>
  );
}

function FilterSelect({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value?: string;
  options: string[];
  onChange: (v: string | undefined) => void;
}) {
  return (
    <select
      value={value ?? ""}
      onChange={(e) => onChange(e.target.value || undefined)}
      className="rounded-lg border border-border bg-card px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-primary"
    >
      <option value="">{label}</option>
      {options.map((o) => (
        <option key={o} value={o}>
          {o}
        </option>
      ))}
    </select>
  );
}
