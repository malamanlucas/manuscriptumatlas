"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { BibleNavigation } from "@/components/bible/BibleNavigation";
import { InterlinearView } from "@/components/bible/InterlinearView";
import { useBibleVersions, useBibleBooks, useBibleInterlinearChapter, useBibleChapter } from "@/hooks/useBible";

export default function InterlinearPage() {
  const t = useTranslations("bible");
  const [selectedVersion, setSelectedVersion] = useState("KJV");
  const [selectedBook, setSelectedBook] = useState("John");
  const [selectedChapter, setSelectedChapter] = useState(1);

  const versionsQuery = useBibleVersions();
  const booksQuery = useBibleBooks();
  const interlinearQuery = useBibleInterlinearChapter(selectedBook, selectedChapter);
  const chapterQuery = useBibleChapter(selectedVersion, selectedBook, selectedChapter);

  const currentBook = booksQuery.data?.find((b) => b.name === selectedBook);

  return (
    <div className="min-h-screen">
      <Header title={t("interlinearTitle")} subtitle={t("interlinearSubtitle")} />
      <div className="mx-auto w-full max-w-6xl p-4 md:p-6 space-y-4">
        <BibleNavigation
          versions={versionsQuery.data ?? []}
          books={booksQuery.data ?? []}
          selectedVersion={selectedVersion}
          selectedBook={selectedBook}
          selectedChapter={selectedChapter}
          totalChapters={currentBook?.totalChapters ?? 1}
          onVersionChange={setSelectedVersion}
          onBookChange={(name) => { setSelectedBook(name); setSelectedChapter(1); }}
          onChapterChange={setSelectedChapter}
        />
        <InterlinearView
          data={interlinearQuery.data}
          isLoading={interlinearQuery.isLoading}
          error={interlinearQuery.error}
          verseTexts={chapterQuery.data?.verses}
          versionLabel={selectedVersion}
        />
      </div>
    </div>
  );
}
