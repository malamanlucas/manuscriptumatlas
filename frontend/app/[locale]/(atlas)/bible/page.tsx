"use client";

import { useState, useCallback, useEffect } from "react";
import { useTranslations } from "next-intl";
import { useLocale } from "next-intl";
import { Header } from "@/components/layout/Header";
import { BibleNavigation } from "@/components/bible/BibleNavigation";
import { LinkedVerseReader } from "@/components/bible/LinkedVerseReader";
import { StudySidebar } from "@/components/bible/StudySidebar";
import {
  useBibleVersions,
  useBibleBooks,
  useBibleChapter,
  useBibleInterlinearChapter,
  useBibleSearch,
} from "@/hooks/useBible";
import { Search } from "lucide-react";
import type { InterlinearWordDTO } from "@/types";
import { getDisplayGloss } from "@/lib/bible/gloss";

export default function BiblePage() {
  const t = useTranslations("bible");
  const locale = useLocale();

  // Navigation state
  const [primaryVersion, setPrimaryVersion] = useState("KJV");
  const [compareVersions, setCompareVersions] = useState<string[]>([]);
  const [selectedBook, setSelectedBook] = useState("John");
  const [selectedChapter, setSelectedChapter] = useState(1);
  const [interlinearMode, setInterlinearMode] = useState(false);
  const [alignVersion, setAlignVersion] = useState("KJV");

  // Mapeamento idioma → versão de alinhamento
  const ALIGN_BY_LANG: Record<string, string> = { en: "KJV", pt: "ARC69" };

  // Search state
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  // Keyboard shortcut: Ctrl+I / Cmd+I
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "i") {
        e.preventDefault();
        setInterlinearMode((prev) => !prev);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  // Data queries
  const versionsQuery = useBibleVersions();
  const booksQuery = useBibleBooks();
  const primaryChapter = useBibleChapter(primaryVersion, selectedBook, selectedChapter);
  const interlinearChapter = useBibleInterlinearChapter(selectedBook, selectedChapter, alignVersion);

  // Compare queries
  const compareChapter1 = useBibleChapter(
    compareVersions[0] ?? "", selectedBook, selectedChapter
  );
  const compareChapter2 = useBibleChapter(
    compareVersions[1] ?? "", selectedBook, selectedChapter
  );

  // Search
  const searchResults = useBibleSearch(debouncedSearch, { locale });

  const currentBook = booksQuery.data?.find((b) => (b.canonicalName ?? b.name) === selectedBook);
  const allVersions = versionsQuery.data ?? [];
  const otherVersions = allVersions.filter((v) => v.code !== primaryVersion);
  const primaryLang = allVersions.find((v) => v.code === primaryVersion)?.language ?? "en";

  // Auto-selecionar alinhamento com base no idioma da versão primária
  useEffect(() => {
    if (ALIGN_BY_LANG[primaryLang]) {
      setAlignVersion(ALIGN_BY_LANG[primaryLang]);
    }
  }, [primaryLang]);

  const handleSearch = useCallback((q: string) => {
    setSearchQuery(q);
    const timer = setTimeout(() => setDebouncedSearch(q), 400);
    return () => clearTimeout(timer);
  }, []);

  const toggleCompareVersion = (code: string) => {
    setCompareVersions((prev) =>
      prev.includes(code)
        ? prev.filter((c) => c !== code)
        : prev.length < 2
          ? [...prev, code]
          : prev
    );
  };

  // Build interlinear data map: verseNumber → { words, kjvText }
  // Gloss language follows the alignment version, not the UI locale.
  // Word data passes through unchanged; getDisplayGloss(word, alignLang) chooses the right gloss at render time.
  const alignLang = allVersions.find((v) => v.code === alignVersion)?.language ?? "en";
  const interlinearByVerse = new Map<number, { words: InterlinearWordDTO[]; kjvText?: string | null }>();
  let hasAnyAlignment = false;

  interlinearChapter.data?.verses.forEach((v) => {
    if (v.words.some((w) => w.kjvAlignment)) {
      hasAnyAlignment = true;
    }
    interlinearByVerse.set(v.verseNumber, {
      words: v.words,
      kjvText: v.kjvText,
    });
  });

  // Build compare text maps
  const compareTexts: { version: string; textByVerse: Map<number, string> }[] = [];
  if (compareVersions[0] && compareChapter1.data) {
    const m = new Map<number, string>();
    compareChapter1.data.verses.forEach((v) => m.set(v.verseNumber, v.text));
    compareTexts.push({ version: compareVersions[0], textByVerse: m });
  }
  if (compareVersions[1] && compareChapter2.data) {
    const m = new Map<number, string>();
    compareChapter2.data.verses.forEach((v) => m.set(v.verseNumber, v.text));
    compareTexts.push({ version: compareVersions[1], textByVerse: m });
  }

  return (
    <div className="min-h-screen">
      <Header title={t("title")} subtitle={t("subtitle")} />

      <div className="mx-auto w-full max-w-7xl p-4 md:p-6">
        <div className="flex flex-col lg:flex-row gap-4">
          {/* Main content */}
          <div className="flex-1 min-w-0 space-y-4">
            {/* Search bar */}
            <div className="rounded-xl border border-border bg-card p-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => handleSearch(e.target.value)}
                  placeholder={t("searchPlaceholder")}
                  className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
                />
              </div>

              {/* Reference detection result */}
              {debouncedSearch.length >= 2 && searchResults.data?.isReference && searchResults.data.resolvedReference && (
                <div className="mt-3 rounded-lg bg-primary/5 border border-primary/20 px-4 py-2.5">
                  <p className="text-xs text-muted-foreground mb-1">{t("referenceDetected")}</p>
                  <button
                    onClick={() => {
                      const ref = searchResults.data!.resolvedReference!;
                      setSelectedBook(ref.book);
                      setSelectedChapter(ref.chapter);
                      setSearchQuery("");
                      setDebouncedSearch("");
                    }}
                    className="text-base font-semibold text-primary hover:underline"
                  >
                    {searchResults.data.resolvedReference.book} {searchResults.data.resolvedReference.chapter}
                    {searchResults.data.resolvedReference.verseStart ? `:${searchResults.data.resolvedReference.verseStart}` : ""}
                  </button>
                </div>
              )}

              {/* Text search results */}
              {debouncedSearch.length >= 2 && searchResults.data && !searchResults.data.isReference && searchResults.data.results.length > 0 && (
                <div className="mt-3 max-h-60 overflow-y-auto space-y-2">
                  <p className="text-xs text-muted-foreground">{t("searchResults", { count: searchResults.data.totalResults })}</p>
                  {searchResults.data.results.slice(0, 10).map((r, idx) => (
                    <button
                      key={idx}
                      onClick={() => {
                        setSelectedBook(r.book);
                        setSelectedChapter(r.chapter);
                        setPrimaryVersion(r.version);
                        setSearchQuery("");
                        setDebouncedSearch("");
                      }}
                      className="block w-full text-left rounded-lg bg-muted/30 px-3 py-2 hover:bg-muted/50 transition-colors"
                    >
                      <span className="text-xs font-semibold text-primary">{r.book} {r.chapter}:{r.verseNumber}</span>
                      <span className="ml-2 text-xs text-muted-foreground">[{r.version}]</span>
                      <p className="text-sm text-foreground/80 mt-0.5 line-clamp-1">{r.snippet ?? r.text}</p>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Navigation + Version selector */}
            <BibleNavigation
              versions={allVersions}
              books={booksQuery.data ?? []}
              selectedVersion={primaryVersion}
              selectedBook={selectedBook}
              selectedChapter={selectedChapter}
              totalChapters={currentBook?.totalChapters ?? 1}
              onVersionChange={setPrimaryVersion}
              onBookChange={(name) => { setSelectedBook(name); setSelectedChapter(1); }}
              onChapterChange={setSelectedChapter}
            />

            {/* Compare version toggles */}
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-medium text-muted-foreground">{t("versionsToCompare")}:</span>
              {otherVersions.map((v) => (
                <button
                  key={v.code}
                  onClick={() => toggleCompareVersion(v.code)}
                  className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                    compareVersions.includes(v.code)
                      ? "border-primary bg-primary/10 text-primary"
                      : "border-border text-muted-foreground hover:border-primary/50"
                  }`}
                >
                  {v.code}
                </button>
              ))}
            </div>

            {/* Main reader */}
            <LinkedVerseReader
              primaryVersion={primaryVersion}
              primaryVerses={primaryChapter.data?.verses ?? []}
              interlinearByVerse={interlinearByVerse}
              compareTexts={compareTexts}
              book={selectedBook}
              chapter={selectedChapter}
              totalChapters={currentBook?.totalChapters}
              isLoading={primaryChapter.isLoading}
              error={primaryChapter.error}
              interlinearMode={interlinearMode}
              alignVersion={alignVersion}
              alignLang={alignLang}
              onPrevChapter={() => setSelectedChapter((c) => Math.max(1, c - 1))}
              onNextChapter={() => setSelectedChapter((c) => Math.min(currentBook?.totalChapters ?? c, c + 1))}
            />
          </div>

          {/* Study sidebar */}
          <div className="w-full lg:w-64 shrink-0">
            <div className="lg:sticky lg:top-4">
              <StudySidebar
                interlinearMode={interlinearMode}
                onToggleInterlinear={() => setInterlinearMode((prev) => !prev)}
                hasAlignmentData={hasAnyAlignment}
                alignVersion={alignVersion}
                onAlignVersionChange={setAlignVersion}
                primaryLang={primaryLang}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
