"use client";

import { useTranslations } from "next-intl";
import { Loader2 } from "lucide-react";
import Link from "next/link";
import type { InterlinearChapterDTO, BibleVerseTextDTO } from "@/types";

interface InterlinearViewProps {
  data?: InterlinearChapterDTO;
  isLoading: boolean;
  error: Error | null;
  verseTexts?: BibleVerseTextDTO[];
  versionLabel?: string;
}

export function InterlinearView({ data, isLoading, error, verseTexts, versionLabel }: InterlinearViewProps) {
  const t = useTranslations("bible");
  const tc = useTranslations("common");

  if (isLoading) {
    return (
      <div className="rounded-xl border border-border bg-card p-12 text-center">
        <Loader2 className="mx-auto h-6 w-6 animate-spin text-muted-foreground" />
        <p className="mt-2 text-sm text-muted-foreground">{tc("loading")}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
        {tc("failedToLoad", { error: error.message })}
      </div>
    );
  }

  if (!data) {
    return (
      <div className="rounded-xl border border-border bg-card p-12 text-center text-muted-foreground">
        {t("noInterlinearData")}
      </div>
    );
  }

  const textByVerse = new Map<number, string>();
  verseTexts?.forEach((v) => textByVerse.set(v.verseNumber, v.text));

  return (
    <div className="space-y-6">
      {data.verses.map((verse) => {
        const translationText = textByVerse.get(verse.verseNumber);
        return (
          <div key={verse.verseNumber} className="rounded-xl border border-border bg-card p-4 md:p-6">
            <div className="mb-3 flex items-center gap-2">
              <span className="rounded-md bg-primary/10 px-2 py-0.5 text-sm font-semibold text-primary">
                {verse.chapter}:{verse.verseNumber}
              </span>
            </div>

            {/* Translation text */}
            {translationText && (
              <div className="mb-4 rounded-lg bg-muted/40 px-4 py-2.5">
                <p className="text-sm leading-relaxed">
                  {versionLabel && (
                    <span className="mr-1.5 text-[10px] font-bold text-primary/60 uppercase">{versionLabel}</span>
                  )}
                  {translationText}
                </p>
              </div>
            )}

            {/* Interlinear words */}
            <div className="flex flex-wrap gap-2">
              {verse.words.map((word) => (
                <div
                  key={word.wordPosition}
                  className="flex flex-col items-center rounded-lg border border-border bg-muted/20 px-2.5 py-2 text-center min-w-[70px] hover:bg-muted/40 transition-colors"
                >
                  <span className="text-base font-semibold leading-tight text-primary" dir={word.language === "hebrew" ? "rtl" : "ltr"}>
                    {word.originalWord}
                  </span>
                  {word.transliteration && (
                    <span className="mt-0.5 text-[11px] italic text-muted-foreground">{word.transliteration}</span>
                  )}
                  {word.englishGloss && (
                    <span className="mt-1 text-[11px] font-semibold text-blue-400">{word.englishGloss}</span>
                  )}
                  <span className="mt-0.5 text-[10px] text-foreground/60">{word.lemma}</span>
                  {word.morphology && (
                    <span className="mt-0.5 text-[10px] text-muted-foreground font-mono">{word.morphology}</span>
                  )}
                  {word.strongsNumber && (
                    <Link
                      href={`/bible/strongs/${word.strongsNumber}`}
                      className="mt-1 text-[10px] font-medium text-blue-400 hover:underline"
                    >
                      {word.strongsNumber}
                    </Link>
                  )}
                </div>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}
