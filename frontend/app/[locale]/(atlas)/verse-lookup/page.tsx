"use client";

import { useState, useEffect, useCallback, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { getManuscriptsForVerse } from "@/lib/api";
import { parseVerseReference } from "@/lib/verseReference";
import { toRoman } from "@/lib/utils";
import { ExternalLink, Search } from "lucide-react";
import type { VerseManuscriptsResponse } from "@/types";

function VerseLookupContent() {
  const t = useTranslations("verseLookup");
  const searchParams = useSearchParams();
  const [input, setInput] = useState("");
  const [type, setType] = useState<string | undefined>(undefined);
  const [result, setResult] = useState<VerseManuscriptsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const doSearch = useCallback(
    async (book: string, chapter: number, verse: number) => {
      setLoading(true);
      setError(null);
      setResult(null);
      setSearched(true);
      try {
        const data = await getManuscriptsForVerse(book, chapter, verse, type);
        setResult(data);
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    },
    [type]
  );

  useEffect(() => {
    const book = searchParams.get("book");
    const chapter = searchParams.get("chapter");
    const verse = searchParams.get("verse");
    if (book && chapter && verse) {
      const ch = parseInt(chapter, 10);
      const v = parseInt(verse, 10);
      if (!isNaN(ch) && !isNaN(v)) {
        setInput(`${book} ${ch}:${v}`);
        doSearch(book, ch, v);
      }
    }
  }, [searchParams, doSearch]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = parseVerseReference(input);
    if (!parsed) {
      setError(t("invalidReference"));
      setResult(null);
      setSearched(true);
      return;
    }
    setError(null);
    doSearch(parsed.book, parsed.chapter, parsed.verse);
  };

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6 space-y-6">
        <div className="rounded-xl border border-border bg-card p-6">
          <p className="text-sm text-muted-foreground mb-4">
            {t("instruction")}
          </p>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="flex-1">
              <label htmlFor="ref" className="sr-only">
                {t("reference")}
              </label>
              <input
                id="ref"
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder={t("placeholder")}
                className="w-full rounded-lg border border-input bg-background px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div className="flex gap-2 flex-wrap items-center">
              {[undefined, "papyrus", "uncial"].map((tp) => (
                <button
                  key={tp ?? "all"}
                  type="button"
                  onClick={() => setType(tp)}
                  className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
                    type === tp
                      ? "bg-primary text-primary-foreground"
                      : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
                >
                  {tp === undefined ? t("allTypes") : tp === "papyrus" ? t("papyri") : t("uncials")}
                </button>
              ))}
              <button
                type="submit"
                disabled={loading}
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
              >
                <Search className="h-4 w-4" />
                {t("search")}
              </button>
            </div>
          </form>
        </div>

        {loading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            {t("loading")}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {error}
          </div>
        )}

        {searched && !loading && !error && result && (
          <div className="rounded-xl border border-border bg-card overflow-hidden">
            <div className="px-6 py-4 border-b border-border bg-muted/30">
              <h2 className="text-lg font-semibold">
                {result.book} {result.chapter}:{result.verse}
              </h2>
              <p className="text-sm text-muted-foreground">
                {t("manuscriptsContaining", { count: result.manuscripts.length })}
              </p>
            </div>
            {result.manuscripts.length === 0 ? (
              <div className="p-12 text-center text-muted-foreground">
                {t("noManuscriptsFound")}
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border bg-muted/50">
                      <th className="px-4 py-3 text-left text-sm font-medium">{t("gaId")}</th>
                      <th className="px-4 py-3 text-left text-sm font-medium">{t("name")}</th>
                      <th className="px-4 py-3 text-left text-sm font-medium">{t("century")}</th>
                      <th className="px-4 py-3 text-left text-sm font-medium">{t("type")}</th>
                      <th className="px-4 py-3 text-right text-sm font-medium"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.manuscripts.map((m) => (
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
                        <td className="px-4 py-3 text-sm capitalize">{m.type ?? "—"}</td>
                        <td className="px-4 py-3 text-right">
                          {m.ntvmrUrl ? (
                            <a
                              href={m.ntvmrUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-sm font-medium text-primary hover:bg-primary/10"
                            >
                              NTVMR <ExternalLink className="h-3 w-3" />
                            </a>
                          ) : (
                            <span className="text-muted-foreground text-sm">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {searched && !loading && !error && !result && (
          <div className="rounded-xl border border-border bg-card p-12 text-center text-muted-foreground">
            {t("enterReference")}
          </div>
        )}
      </div>
    </div>
  );
}

export default function VerseLookupPage() {
  const t = useTranslations("verseLookup");
  return (
    <Suspense
      fallback={
        <div className="min-h-screen">
          <Header
            title={t("title")}
            subtitle={t("subtitle")}
          />
          <div className="p-4 md:p-6">
            <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
              {t("loading")}
            </div>
          </div>
        </div>
      }
    >
      <VerseLookupContent />
    </Suspense>
  );
}
