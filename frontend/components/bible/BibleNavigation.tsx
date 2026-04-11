"use client";

import { useTranslations } from "next-intl";
import { ChevronLeft, ChevronRight } from "lucide-react";
import type { BibleVersionDTO, BibleBookDTO } from "@/types";

interface BibleNavigationProps {
  versions: BibleVersionDTO[];
  books: BibleBookDTO[];
  selectedVersion: string;
  selectedBook: string;
  selectedChapter: number;
  totalChapters: number;
  onVersionChange: (version: string) => void;
  onBookChange: (book: string) => void;
  onChapterChange: (chapter: number) => void;
}

export function BibleNavigation({
  versions,
  books,
  selectedVersion,
  selectedBook,
  selectedChapter,
  totalChapters,
  onVersionChange,
  onBookChange,
  onChapterChange,
}: BibleNavigationProps) {
  const t = useTranslations("bible");

  const otBooks = books.filter((b) => b.testament === "OT");
  const ntBooks = books.filter((b) => b.testament === "NT");

  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-4">
        {/* Version selector */}
        <div className="flex-shrink-0">
          <label className="mb-1 block text-xs font-medium text-muted-foreground">
            {t("version")}
          </label>
          <select
            value={selectedVersion}
            onChange={(e) => onVersionChange(e.target.value)}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm sm:w-auto"
          >
            {versions.map((v) => (
              <option key={v.code} value={v.code}>
                {v.code} — {v.name}
              </option>
            ))}
          </select>
        </div>

        {/* Book selector */}
        <div className="flex-1">
          <label className="mb-1 block text-xs font-medium text-muted-foreground">
            {t("book")}
          </label>
          <select
            value={selectedBook}
            onChange={(e) => onBookChange(e.target.value)}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
          >
            {otBooks.length > 0 && (
              <optgroup label={t("oldTestament")}>
                {otBooks.map((b) => (
                  <option key={b.id} value={b.name}>
                    {b.name}
                  </option>
                ))}
              </optgroup>
            )}
            {ntBooks.length > 0 && (
              <optgroup label={t("newTestament")}>
                {ntBooks.map((b) => (
                  <option key={b.id} value={b.name}>
                    {b.name}
                  </option>
                ))}
              </optgroup>
            )}
          </select>
        </div>

        {/* Chapter navigation */}
        <div className="flex-shrink-0">
          <label className="mb-1 block text-xs font-medium text-muted-foreground">
            {t("chapter")}
          </label>
          <div className="flex items-center gap-2">
            <button
              onClick={() => onChapterChange(Math.max(1, selectedChapter - 1))}
              disabled={selectedChapter <= 1}
              className="rounded-lg border border-border p-2 text-muted-foreground hover:bg-muted disabled:opacity-30"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <select
              value={selectedChapter}
              onChange={(e) => onChapterChange(Number(e.target.value))}
              className="w-20 rounded-lg border border-border bg-background px-3 py-2 text-center text-sm"
            >
              {Array.from({ length: totalChapters }, (_, i) => i + 1).map((ch) => (
                <option key={ch} value={ch}>
                  {ch}
                </option>
              ))}
            </select>
            <button
              onClick={() => onChapterChange(Math.min(totalChapters, selectedChapter + 1))}
              disabled={selectedChapter >= totalChapters}
              className="rounded-lg border border-border p-2 text-muted-foreground hover:bg-muted disabled:opacity-30"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
