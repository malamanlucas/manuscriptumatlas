"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useApologeticTopics, useSearchApologeticTopics } from "@/hooks/useApologetics";
import { useAuth } from "@/hooks/useAuth";
import { Link } from "@/i18n/navigation";
import { Search, Plus, ShieldQuestion } from "lucide-react";

const STATUS_COLORS: Record<string, string> = {
  DRAFT: "bg-yellow-500/20 text-yellow-700 dark:text-yellow-400",
  PUBLISHED: "bg-green-500/20 text-green-700 dark:text-green-400",
  ARCHIVED: "bg-gray-500/20 text-gray-500",
};

export default function ApologeticsPage() {
  const t = useTranslations("apologetics");
  const tc = useTranslations("common");
  const { isAdmin } = useAuth();

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  const [page, setPage] = useState(1);
  const limit = 25;

  const listQuery = useApologeticTopics({
    page,
    limit,
    status: statusFilter === "all" ? undefined : statusFilter,
  });
  const searchQuery = useSearchApologeticTopics(search, 50);

  const topics = useMemo(() => {
    if (search.trim().length >= 2) return searchQuery.data ?? [];
    return listQuery.data?.topics ?? [];
  }, [search, searchQuery.data, listQuery.data]);

  const total = listQuery.data?.total ?? 0;

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-5xl space-y-6 p-4 md:p-6">
        <div className="space-y-4 rounded-xl border border-border bg-card p-4 md:p-6">
          <div className="flex flex-col gap-3 md:flex-row md:items-end">
            <div>
              <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("status")}</label>
              <div className="flex flex-wrap gap-2">
                {["all", "DRAFT", "PUBLISHED", "ARCHIVED"].map((s) => (
                  <button
                    key={s}
                    onClick={() => { setStatusFilter(s); setPage(1); }}
                    className={`rounded-lg px-3 py-2 text-sm font-medium ${statusFilter === s ? "bg-primary text-primary-foreground" : "bg-secondary text-secondary-foreground hover:bg-secondary/80"}`}
                  >
                    {s === "all" ? tc("all") : t(s.toLowerCase() as "draft" | "published" | "archived")}
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

            {isAdmin && (
              <Link
                href="/apologetics/new"
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                <Plus className="h-4 w-4" />
                {t("newTopic")}
              </Link>
            )}
          </div>
        </div>

        <div className="rounded-xl border border-border bg-card p-4 md:p-6">
          {listQuery.isLoading && <div className="text-sm text-muted-foreground">{tc("loading")}</div>}
          {listQuery.error && <div className="text-sm text-red-500">{(listQuery.error as Error).message}</div>}
          {!listQuery.isLoading && topics.length === 0 && (
            <div className="flex flex-col items-center gap-3 py-12 text-muted-foreground">
              <ShieldQuestion className="h-10 w-10 opacity-40" />
              <p className="text-sm">{t("noResults")}</p>
            </div>
          )}

          {topics.length > 0 && (
            <div className="space-y-3">
              {topics.map((topic) => (
                <Link
                  key={topic.id}
                  href={`/apologetics/${topic.slug}`}
                  className="block rounded-lg border border-border/50 p-4 transition-colors hover:bg-secondary/30"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <h3 className="font-medium">{topic.title}</h3>
                      <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                        <span>{new Date(topic.createdAt).toLocaleDateString()}</span>
                        <span>·</span>
                        <span>{t("responseCount", { count: topic.responseCount })}</span>
                      </div>
                    </div>
                    <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLORS[topic.status] ?? ""}`}>
                      {t(topic.status.toLowerCase() as "draft" | "published" | "archived")}
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          )}

          {listQuery.data && search.trim().length < 2 && total > limit && (
            <div className="mt-4 flex items-center justify-between">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page === 1}
                className="rounded-lg bg-secondary px-3 py-1.5 text-sm disabled:opacity-50"
              >
                {tc("previous")}
              </button>
              <span className="text-sm text-muted-foreground">
                {page} / {Math.ceil(total / limit)}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page * limit >= total}
                className="rounded-lg bg-secondary px-3 py-1.5 text-sm disabled:opacity-50"
              >
                {tc("next")}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
