"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  useAnalyticsVisitors,
  useAnalyticsVisitorSessions,
  useAnalyticsSessionPageviews,
} from "@/hooks/useVisitorAnalytics";
import type { SessionFilters } from "@/types";
import { ChevronLeft, ChevronRight, User, X } from "lucide-react";

export function VisitorsTab({ filters }: { filters: SessionFilters }) {
  const t = useTranslations("observatory");
  const [page, setPage] = useState(1);
  const [returningFilter, setReturningFilter] = useState<boolean | undefined>();
  const [sort, setSort] = useState("last_seen");
  const [selectedVisitor, setSelectedVisitor] = useState<string | null>(null);

  const { data, isLoading } = useAnalyticsVisitors({
    ...filters,
    returning: returningFilter,
    sort,
    order: "desc",
    page,
    limit: 25,
  });

  const visitors = data?.visitors ?? [];
  const total = data?.total ?? 0;
  const totalPages = Math.ceil(total / 25);

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex rounded-lg border border-border bg-card p-1">
          {([
            { label: t("visitors.all"), value: undefined },
            { label: t("visitors.new"), value: false },
            { label: t("visitors.returning"), value: true },
          ] as const).map(({ label, value }) => (
            <button
              key={label}
              onClick={() => { setReturningFilter(value); setPage(1); }}
              className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                returningFilter === value
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-muted"
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        <select
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          className="rounded-lg border border-border bg-card px-3 py-1.5 text-xs"
        >
          <option value="last_seen">{t("visitors.sortLastSeen")}</option>
          <option value="sessions">{t("visitors.sortSessions")}</option>
          <option value="pageviews">{t("visitors.sortPageviews")}</option>
        </select>
      </div>

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-border bg-card">
        <table className="w-full text-xs">
          <thead>
            <tr className="border-b border-border text-left text-muted-foreground">
              <th className="px-3 py-2.5">{t("table.visitor")}</th>
              <th className="px-3 py-2.5">{t("visitors.sessions")}</th>
              <th className="px-3 py-2.5">{t("visitors.pages")}</th>
              <th className="px-3 py-2.5">{t("visitors.firstSeen")}</th>
              <th className="px-3 py-2.5">{t("visitors.lastSeen")}</th>
              <th className="px-3 py-2.5">{t("table.browser")}</th>
              <th className="px-3 py-2.5">{t("table.device")}</th>
              <th className="px-3 py-2.5">IP</th>
            </tr>
          </thead>
          <tbody>
            {visitors.map((v) => (
              <tr
                key={v.visitorId}
                onClick={() => setSelectedVisitor(v.visitorId)}
                className="cursor-pointer border-b border-border/50 hover:bg-muted/50 transition-colors last:border-0"
              >
                <td className="px-3 py-2 font-mono">
                  {v.visitorId.slice(0, 8)}...
                </td>
                <td className="px-3 py-2 font-semibold">{v.sessionCount}</td>
                <td className="px-3 py-2">{v.totalPageviews}</td>
                <td className="px-3 py-2 whitespace-nowrap">
                  {new Date(v.firstSeenAt).toLocaleDateString()}
                </td>
                <td className="px-3 py-2 whitespace-nowrap">
                  {new Date(v.lastSeenAt).toLocaleString([], {
                    month: "short",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </td>
                <td className="px-3 py-2">{v.lastBrowser ?? "—"}</td>
                <td className="px-3 py-2">{v.lastDeviceType ?? "—"}</td>
                <td className="px-3 py-2 font-mono">{v.lastIp ?? "—"}</td>
              </tr>
            ))}
            {visitors.length === 0 && !isLoading && (
              <tr>
                <td colSpan={8} className="px-3 py-8 text-center text-muted-foreground">
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
            {(page - 1) * 25 + 1}-{Math.min(page * 25, total)} / {total}
          </span>
          <div className="flex items-center gap-2">
            <button
              disabled={page <= 1}
              onClick={() => setPage(page - 1)}
              className="rounded-md border border-border p-1.5 hover:bg-muted disabled:opacity-30"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span>{page} / {totalPages}</span>
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

      {/* Visitor Profile Drawer */}
      {selectedVisitor && (
        <VisitorProfileDrawer
          visitorId={selectedVisitor}
          onClose={() => setSelectedVisitor(null)}
        />
      )}
    </div>
  );
}

function VisitorProfileDrawer({
  visitorId,
  onClose,
}: {
  visitorId: string;
  onClose: () => void;
}) {
  const t = useTranslations("observatory");
  const [sessionPage, setSessionPage] = useState(1);
  const { data: sessions } = useAnalyticsVisitorSessions(
    visitorId,
    sessionPage,
    10
  );

  return (
    <>
      <div
        className="fixed inset-0 z-40 bg-black/50"
        onClick={onClose}
        aria-hidden="true"
      />
      <div className="fixed right-0 top-0 z-50 h-screen w-full max-w-lg overflow-y-auto border-l border-border bg-card shadow-xl">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-card px-4 py-3">
          <div className="flex items-center gap-2">
            <User className="h-4 w-4" />
            <h3 className="text-sm font-semibold">{t("visitors.profile")}</h3>
          </div>
          <button onClick={onClose} className="rounded-md p-1 hover:bg-muted">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="px-4 py-3">
          <p className="mb-2 text-xs text-muted-foreground">Visitor ID</p>
          <p className="mb-4 font-mono text-sm">{visitorId}</p>

          <h4 className="mb-2 text-xs font-semibold uppercase text-muted-foreground">
            {t("visitors.sessions")}
          </h4>

          <div className="space-y-2">
            {(sessions?.sessions ?? []).map((s) => (
              <div
                key={s.sessionId}
                className="rounded-lg border border-border p-3 text-xs"
              >
                <div className="mb-1 flex items-center justify-between">
                  <span className="font-mono">{s.sessionId.slice(0, 12)}...</span>
                  <span className="text-muted-foreground">
                    {new Date(s.createdAt).toLocaleString([], {
                      month: "short",
                      day: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                </div>
                <div className="flex gap-3 text-muted-foreground">
                  <span>{s.browserName ?? "—"}</span>
                  <span>{s.osName ?? "—"}</span>
                  <span>{s.deviceType ?? "—"}</span>
                  <span className="font-mono">{s.ipAddress}</span>
                </div>
              </div>
            ))}
          </div>

          {sessions && sessions.total > 10 && (
            <div className="mt-3 flex items-center justify-center gap-2 text-xs">
              <button
                disabled={sessionPage <= 1}
                onClick={() => setSessionPage(sessionPage - 1)}
                className="rounded-md border border-border p-1.5 hover:bg-muted disabled:opacity-30"
              >
                <ChevronLeft className="h-3 w-3" />
              </button>
              <span>{sessionPage} / {Math.ceil(sessions.total / 10)}</span>
              <button
                disabled={sessionPage >= Math.ceil(sessions.total / 10)}
                onClick={() => setSessionPage(sessionPage + 1)}
                className="rounded-md border border-border p-1.5 hover:bg-muted disabled:opacity-30"
              >
                <ChevronRight className="h-3 w-3" />
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
