"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useHeresies } from "@/hooks/useHeresies";
import { Link } from "@/i18n/navigation";

export default function HeresiesPage() {
  const t = useTranslations("heresies");
  const tc = useTranslations("common");
  const [page, setPage] = useState(1);
  const query = useHeresies({ page, limit: 30 });

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />
      <div className="space-y-6 p-4 md:p-6">
        {query.isLoading && <div className="text-sm text-muted-foreground">{tc("loading")}</div>}
        {query.error && <div className="text-sm text-red-500">{(query.error as Error).message}</div>}

        {!!query.data?.heresies.length && (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {query.data.heresies.map((h) => (
              <Link key={h.id} href={`/heresies/${h.slug}`} className="rounded-xl border border-border bg-card p-4 hover:border-primary/20">
                <p className="font-semibold">{h.name}</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {h.yearOrigin ?? "—"} · {h.centuryOrigin ? `${t("century")} ${h.centuryOrigin}` : "—"}
                </p>
                {h.keyFigure && <p className="mt-2 text-xs text-muted-foreground">{h.keyFigure}</p>}
              </Link>
            ))}
          </div>
        )}

        {query.data && query.data.total > 30 && (
          <div className="flex items-center justify-between">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              className="rounded-lg bg-secondary px-3 py-1.5 text-sm disabled:opacity-50"
            >
              {tc("previous")}
            </button>
            <span className="text-sm text-muted-foreground">
              {page} / {Math.ceil(query.data.total / 30)}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page * 30 >= query.data.total}
              className="rounded-lg bg-secondary px-3 py-1.5 text-sm disabled:opacity-50"
            >
              {tc("next")}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
