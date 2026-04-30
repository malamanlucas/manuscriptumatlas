"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { BibleNavigation } from "@/components/bible/BibleNavigation";
import { VerseCompare } from "@/components/bible/VerseCompare";
import { useBibleVersions, useBibleBooks, useBibleCompare } from "@/hooks/useBible";

export default function ComparePage() {
  const t = useTranslations("bible");
  const [selectedBook, setSelectedBook] = useState("John");
  const [selectedChapter, setSelectedChapter] = useState(3);
  const [selectedVersions, setSelectedVersions] = useState(["KJV", "ARC"]);

  const versionsQuery = useBibleVersions();
  const booksQuery = useBibleBooks();
  const compareQuery = useBibleCompare(selectedBook, selectedChapter, selectedVersions);

  const currentBook = booksQuery.data?.find((b) => (b.canonicalName ?? b.name) === selectedBook);
  const allVersions = versionsQuery.data ?? [];

  return (
    <div className="min-h-screen">
      <Header title={t("compareTitle")} subtitle={t("compareSubtitle")} />
      <div className="mx-auto w-full max-w-6xl p-4 md:p-6 space-y-4">
        <div className="rounded-xl border border-border bg-card p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:gap-4">
            <div className="flex-1">
              <BibleNavigation
                versions={allVersions}
                books={booksQuery.data ?? []}
                selectedVersion=""
                selectedBook={selectedBook}
                selectedChapter={selectedChapter}
                totalChapters={currentBook?.totalChapters ?? 1}
                onVersionChange={() => {}}
                onBookChange={(name) => { setSelectedBook(name); setSelectedChapter(1); }}
                onChapterChange={setSelectedChapter}
              />
            </div>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            <span className="text-xs font-medium text-muted-foreground self-center mr-1">{t("versionsToCompare")}:</span>
            {allVersions.map((v) => (
              <button
                key={v.code}
                onClick={() => {
                  setSelectedVersions((prev) =>
                    prev.includes(v.code)
                      ? prev.filter((c) => c !== v.code)
                      : [...prev, v.code]
                  );
                }}
                className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                  selectedVersions.includes(v.code)
                    ? "border-primary bg-primary/10 text-primary"
                    : "border-border text-muted-foreground hover:border-primary/50"
                }`}
              >
                {v.code}
              </button>
            ))}
          </div>
        </div>

        <VerseCompare
          data={compareQuery.data}
          isLoading={compareQuery.isLoading}
          error={compareQuery.error}
        />
      </div>
    </div>
  );
}
