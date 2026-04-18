"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { useBibleBooks } from "@/hooks/useBible";
import {
  useBibleLayer4Applications,
  useBibleLayer4Coverage,
  useRunBibleScopedPhases,
} from "@/hooks/useBibleLayer4";
import { CheckCircle2, Circle, Loader2, Play, AlertCircle, Target } from "lucide-react";

interface BibleLayer4ScopePanelProps {
  phases: string[];
  phaseLabels: Record<string, string>;
}

export function BibleLayer4ScopePanel({ phases, phaseLabels }: BibleLayer4ScopePanelProps) {
  const t = useTranslations("ingestion.bibleIngestion");
  const booksQuery = useBibleBooks("NT");
  const runScoped = useRunBibleScopedPhases();

  const [selectedBook, setSelectedBook] = useState<string>("");
  const [selectedChapter, setSelectedChapter] = useState<number | null>(null);
  const [selectedVerse, setSelectedVerse] = useState<number | null>(null);
  const [selectedPhases, setSelectedPhases] = useState<Set<string>>(new Set(phases));

  const selectedBookDTO = useMemo(
    () => booksQuery.data?.find((b) => b.name === selectedBook) ?? null,
    [booksQuery.data, selectedBook]
  );

  const coverageQuery = useBibleLayer4Coverage(
    selectedBook || null,
    selectedChapter
  );

  const applicationsQuery = useBibleLayer4Applications({
    book: selectedBook || null,
    chapter: selectedChapter,
    verse: selectedVerse,
    limit: 20,
  });

  const togglePhase = (phase: string) => {
    setSelectedPhases((prev) => {
      const next = new Set(prev);
      if (next.has(phase)) next.delete(phase);
      else next.add(phase);
      return next;
    });
  };

  const handleApply = () => {
    if (!selectedBook) {
      alert(t("layer4ScopeMissingBook"));
      return;
    }
    if (selectedPhases.size === 0) {
      alert(t("layer4ScopeMissingPhases"));
      return;
    }
    runScoped.mutate({
      phases: Array.from(selectedPhases),
      bookName: selectedBook,
      chapter: selectedChapter,
      verse: selectedVerse,
    });
  };

  const handleBookChange = (book: string) => {
    setSelectedBook(book);
    setSelectedChapter(null);
    setSelectedVerse(null);
  };

  const handleChapterChange = (chapterRaw: string) => {
    const parsed = chapterRaw ? Number(chapterRaw) : null;
    setSelectedChapter(parsed);
    setSelectedVerse(null);
  };

  const handleVerseChange = (verseRaw: string) => {
    const parsed = verseRaw ? Number(verseRaw) : null;
    setSelectedVerse(parsed);
  };

  const chapterVerses = coverageQuery.data?.totalVerses ?? 0;

  return (
    <div className="space-y-4 rounded-lg border border-primary/30 bg-primary/5 p-4">
      <div className="flex items-center gap-2">
        <Target className="h-4 w-4 text-primary" />
        <h4 className="text-sm font-bold">{t("layer4ScopeTitle")}</h4>
      </div>

      {/* Escopo: Book / Chapter / Verse */}
      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        <label className="flex flex-col gap-1 text-xs">
          <span className="font-medium text-muted-foreground">
            {t("layer4ScopeBook")}
          </span>
          <select
            value={selectedBook}
            onChange={(e) => handleBookChange(e.target.value)}
            disabled={booksQuery.isLoading}
            className="rounded-md border border-border bg-background px-2 py-1.5 text-sm"
          >
            <option value="">{t("layer4ScopeAllBooks")}</option>
            {booksQuery.data?.map((book) => (
              <option key={book.id} value={book.name}>
                {book.name}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-1 text-xs">
          <span className="font-medium text-muted-foreground">
            {t("layer4ScopeChapter")}
          </span>
          <select
            value={selectedChapter ?? ""}
            onChange={(e) => handleChapterChange(e.target.value)}
            disabled={!selectedBookDTO}
            className="rounded-md border border-border bg-background px-2 py-1.5 text-sm disabled:opacity-50"
          >
            <option value="">{t("layer4ScopeAllChapters")}</option>
            {selectedBookDTO &&
              Array.from({ length: selectedBookDTO.totalChapters }, (_, i) => i + 1).map((ch) => (
                <option key={ch} value={ch}>
                  {ch}
                </option>
              ))}
          </select>
        </label>

        <label className="flex flex-col gap-1 text-xs">
          <span className="font-medium text-muted-foreground">
            {t("layer4ScopeVerse")}
          </span>
          <select
            value={selectedVerse ?? ""}
            onChange={(e) => handleVerseChange(e.target.value)}
            disabled={!selectedChapter || chapterVerses === 0}
            className="rounded-md border border-border bg-background px-2 py-1.5 text-sm disabled:opacity-50"
          >
            <option value="">{t("layer4ScopeAllVerses")}</option>
            {chapterVerses > 0 &&
              Array.from({ length: chapterVerses }, (_, i) => i + 1).map((v) => (
                <option key={v} value={v}>
                  {v}
                </option>
              ))}
          </select>
        </label>
      </div>

      {/* Fases */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-muted-foreground">
            {t("layer4ScopePhases")}
          </span>
          <div className="flex gap-2 text-xs">
            <button
              onClick={() => setSelectedPhases(new Set(phases))}
              className="text-primary hover:underline"
            >
              {t("layer4ScopeSelectAll")}
            </button>
            <button
              onClick={() => setSelectedPhases(new Set())}
              className="text-muted-foreground hover:underline"
            >
              {t("layer4ScopeSelectNone")}
            </button>
          </div>
        </div>
        <div className="grid grid-cols-1 gap-1.5 md:grid-cols-2">
          {phases.map((phase) => (
            <label
              key={phase}
              className="flex cursor-pointer items-center gap-2 rounded-md border border-border bg-background px-2 py-1.5 text-xs hover:bg-accent/50"
            >
              <input
                type="checkbox"
                checked={selectedPhases.has(phase)}
                onChange={() => togglePhase(phase)}
                className="h-3.5 w-3.5"
              />
              <span className="truncate">{phaseLabels[phase] ?? phase}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Aplicar */}
      <div className="flex items-center justify-end gap-2">
        <button
          onClick={handleApply}
          disabled={runScoped.isPending || !selectedBook || selectedPhases.size === 0}
          className="inline-flex items-center gap-1.5 rounded-lg border border-primary bg-primary px-4 py-1.5 text-xs font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
        >
          {runScoped.isPending ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Play className="h-3.5 w-3.5" />
          )}
          {t("layer4ScopeApply")}
        </button>
      </div>

      {/* Cobertura */}
      {selectedBook && selectedChapter && coverageQuery.data && coverageQuery.data.totalVerses > 0 && (
        <div className="space-y-2 rounded-md border border-border bg-background p-3">
          <h5 className="text-xs font-bold">
            {t("layer4CoverageTitle", { book: coverageQuery.data.book, chapter: coverageQuery.data.chapter })}
          </h5>
          <div className="overflow-x-auto">
            <table className="min-w-full text-xs">
              <thead>
                <tr className="border-b border-border text-left text-muted-foreground">
                  <th className="px-2 py-1">{t("layer4CoverageVerse")}</th>
                  <th className="px-2 py-1">tok_arc69</th>
                  <th className="px-2 py-1">tok_kjv</th>
                  <th className="px-2 py-1">aln_kjv</th>
                  <th className="px-2 py-1">aln_arc69</th>
                  <th className="px-2 py-1">sem_arc69</th>
                </tr>
              </thead>
              <tbody>
                {coverageQuery.data.verses.map((v) => (
                  <tr
                    key={v.verse}
                    className={`border-b border-border/40 ${selectedVerse === v.verse ? "bg-primary/10" : ""}`}
                  >
                    <td className="px-2 py-1 font-mono">{v.verse}</td>
                    <CoverageCell value={v.tokenizeArc69} />
                    <CoverageCell value={v.tokenizeKjv} />
                    <CoverageCell value={v.alignKjv} />
                    <CoverageCell value={v.alignArc69} />
                    <CoverageCell value={v.enrichSemanticsArc69} />
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {selectedBook && selectedChapter && coverageQuery.data && coverageQuery.data.totalVerses === 0 && (
        <p className="text-xs text-muted-foreground">{t("layer4CoverageEmpty")}</p>
      )}

      {/* Histórico */}
      <div className="rounded-md border border-border bg-background">
        <div className="flex items-center justify-between border-b border-border/60 px-3 py-2">
          <h5 className="text-xs font-bold">{t("layer4HistoryTitle")}</h5>
          {applicationsQuery.data && applicationsQuery.data.length > 0 && (
            <span className="text-[11px] tabular-nums text-muted-foreground">
              {applicationsQuery.data.length}
            </span>
          )}
        </div>
        <div className="max-h-60 overflow-y-auto px-3 py-2">
          {applicationsQuery.isLoading && (
            <p className="text-xs text-muted-foreground">…</p>
          )}
          {applicationsQuery.data && applicationsQuery.data.length === 0 && (
            <p className="text-xs text-muted-foreground">{t("layer4HistoryEmpty")}</p>
          )}
          {applicationsQuery.data && applicationsQuery.data.length > 0 && (
            <ul className="space-y-1">
              {applicationsQuery.data.map((app) => (
                <li
                  key={app.id}
                  className="flex items-center justify-between gap-2 rounded border border-border/40 px-2 py-1 text-xs"
                >
                  <span className="flex min-w-0 items-center gap-2">
                    <StatusIcon status={app.status} />
                    <span className="truncate font-mono text-[11px]">
                      {formatScope(app.bookName, app.chapter, app.verse)}
                    </span>
                    <span className="truncate text-muted-foreground">{app.phaseName}</span>
                  </span>
                  <span className="shrink-0 text-[11px] text-muted-foreground">
                    {app.itemsProcessed} / {new Date(app.requestedAt).toLocaleTimeString()}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function CoverageCell({ value }: { value: boolean }) {
  return (
    <td className="px-2 py-1">
      {value ? (
        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
      ) : (
        <Circle className="h-3.5 w-3.5 text-muted-foreground/40" />
      )}
    </td>
  );
}

function StatusIcon({ status }: { status: string }) {
  if (status === "success") return <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-emerald-500" />;
  if (status === "failed") return <AlertCircle className="h-3.5 w-3.5 shrink-0 text-red-500" />;
  return <Loader2 className="h-3.5 w-3.5 shrink-0 animate-spin text-primary" />;
}

function formatScope(book: string | null, chapter: number | null, verse: number | null): string {
  if (!book) return "—";
  if (chapter == null) return book;
  if (verse == null) return `${book} ${chapter}`;
  return `${book} ${chapter}:${verse}`;
}
