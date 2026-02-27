"use client";

import { useState } from "react";
import Link from "next/link";
import { Header } from "@/components/layout/Header";
import { useManuscripts } from "@/hooks/useManuscripts";
import { toRoman } from "@/lib/utils";
import { ExternalLink } from "lucide-react";

export default function ManuscriptsPage() {
  const [type, setType] = useState<string | undefined>("papyrus");
  const [century, setCentury] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(1);

  const { data, isLoading, error } = useManuscripts({
    type,
    century,
    page,
    limit: 50,
  });

  return (
    <div className="min-h-screen">
      <Header
        title="Manuscript Explorer"
        subtitle="Browse papyri and uncial manuscripts"
      />

      <div className="p-6 space-y-6">
        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div className="flex gap-2">
              <button
                onClick={() => setType(undefined)}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  !type
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                All
              </button>
              <button
                onClick={() => { setType("papyrus"); setPage(1); }}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "papyrus"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                Papyri
              </button>
              <button
                onClick={() => { setType("uncial"); setPage(1); }}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                  type === "uncial"
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                Uncials
              </button>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">Century:</span>
              <select
                value={century ?? ""}
                onChange={(e) => {
                  const v = e.target.value;
                  setCentury(v ? parseInt(v, 10) : undefined);
                  setPage(1);
                }}
                className="rounded-lg border border-input bg-background px-3 py-1.5 text-sm"
              >
                <option value="">All</option>
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((c) => (
                  <option key={c} value={c}>
                    {toRoman(c)}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            Loading manuscripts...
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            Failed to load: {(error as Error).message}
          </div>
        )}

        {data && data.length > 0 && (
          <div className="rounded-xl border border-border bg-card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-border bg-muted/50">
                    <th className="px-4 py-3 text-left text-sm font-medium">GA ID</th>
                    <th className="px-4 py-3 text-left text-sm font-medium">Name</th>
                    <th className="px-4 py-3 text-left text-sm font-medium">Century</th>
                    <th className="px-4 py-3 text-left text-sm font-medium">Type</th>
                    <th className="px-4 py-3 text-right text-sm font-medium">Books</th>
                    <th className="px-4 py-3 text-right text-sm font-medium">Verses</th>
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((m) => (
                    <tr
                      key={m.gaId}
                      className="border-b border-border last:border-0 hover:bg-muted/30"
                    >
                      <td className="px-4 py-3 font-mono font-medium">{m.gaId}</td>
                      <td className="px-4 py-3 text-sm">{m.name ?? "—"}</td>
                      <td className="px-4 py-3 text-sm">
                        {m.centuryMin === m.centuryMax
                          ? toRoman(m.centuryMin)
                          : `${toRoman(m.centuryMin)}/${toRoman(m.centuryMax)}`}
                      </td>
                      <td className="px-4 py-3 text-sm capitalize">{m.manuscriptType ?? "—"}</td>
                      <td className="px-4 py-3 text-right">{m.bookCount}</td>
                      <td className="px-4 py-3 text-right">{m.verseCount.toLocaleString()}</td>
                      <td className="px-4 py-3">
                        <Link
                          href={`/manuscripts/${m.gaId}`}
                          className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-sm font-medium text-primary hover:bg-primary/10"
                        >
                          View <ExternalLink className="h-3 w-3" />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex justify-between items-center px-4 py-3 border-t border-border bg-muted/30">
              <span className="text-sm text-muted-foreground">
                Page {page} · {data.length} items
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page <= 1}
                  className="rounded-lg px-3 py-1.5 text-sm font-medium bg-secondary disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.length < 50}
                  className="rounded-lg px-3 py-1.5 text-sm font-medium bg-secondary disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          </div>
        )}

        {data && data.length === 0 && !isLoading && (
          <div className="rounded-xl border border-border bg-card p-12 text-center text-muted-foreground">
            No manuscripts found for the selected filters.
          </div>
        )}
      </div>
    </div>
  );
}
