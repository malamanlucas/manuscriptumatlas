"use client";

import type { LexiconEntryDTO } from "@/types";
import { useTranslations } from "next-intl";

interface StrongsEntryProps {
  entry: LexiconEntryDTO;
}

function parseDefinition(text: string): { intro: string; definitions: { label: string; text: string }[] } {
  const parts = text.split(/(_{2,}\d+\.|_{2,}[IVX]+\.|_{2,}\([a-z]\))/);
  if (parts.length <= 1) return { intro: text, definitions: [] };

  const intro = parts[0].trim();
  const definitions: { label: string; text: string }[] = [];
  for (let i = 1; i < parts.length; i += 2) {
    const label = parts[i]?.replace(/_/g, "").trim() || "";
    const content = parts[i + 1]?.trim() || "";
    if (label && content) definitions.push({ label, text: content });
  }
  return { intro, definitions };
}

export function StrongsEntry({ entry }: StrongsEntryProps) {
  const t = useTranslations("bible");
  const parsed = entry.fullDefinition ? parseDefinition(entry.fullDefinition) : null;

  return (
    <div className="space-y-6">
      {/* Header: Number + Lemma */}
      <div className="rounded-xl border border-border bg-card p-5 md:p-8">
        <div className="flex items-baseline gap-3 mb-6">
          <span className="text-3xl font-bold text-muted-foreground">
            {entry.strongsNumber.replace("G", "").replace("H", "")}.
          </span>
          <span className="text-3xl font-semibold">
            {entry.transliteration ?? entry.lemma}
          </span>
        </div>

        {/* Lexical Summary */}
        <div className="space-y-4">
          <h3 className="text-lg font-bold border-b border-border pb-2">{t("lexicalSummary")}</h3>

          {entry.shortDefinition && (
            <p className="text-base">
              <span className="font-semibold">{entry.transliteration ?? entry.lemma}:</span>{" "}
              <span className="text-foreground/80 capitalize">{entry.shortDefinition}</span>
            </p>
          )}

          <div className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
            <span className="font-semibold text-red-500 dark:text-red-400">{t("originalWord")}:</span>
            <span className="text-lg" dir={entry.language === "hebrew" ? "rtl" : "ltr"}>{entry.lemma}</span>

            {entry.partOfSpeech && (
              <>
                <span className="font-semibold text-red-500 dark:text-red-400">{t("partOfSpeech")}:</span>
                <span>{formatPartOfSpeech(entry.partOfSpeech)}</span>
              </>
            )}

            {entry.transliteration && (
              <>
                <span className="font-semibold text-red-500 dark:text-red-400">{t("transliterationLabel")}:</span>
                <span>{entry.transliteration}</span>
              </>
            )}

            {entry.pronunciation && (
              <>
                <span className="font-semibold text-red-500 dark:text-red-400">{t("pronunciationLabel")}:</span>
                <span>{entry.pronunciation}</span>
              </>
            )}

            {entry.phoneticSpelling && (
              <>
                <span className="font-semibold text-red-500 dark:text-red-400">{t("phoneticSpelling")}:</span>
                <span>{entry.phoneticSpelling}</span>
              </>
            )}

            {entry.kjvTranslation && (
              <>
                <span className="font-semibold text-red-500 dark:text-red-400">KJV:</span>
                <span>
                  {entry.kjvTranslation}
                  {entry.kjvUsageCount != null && (
                    <span className="ml-1 text-muted-foreground">({entry.kjvUsageCount}x)</span>
                  )}
                </span>
              </>
            )}

            {entry.nasbTranslation && (
              <>
                <span className="font-semibold text-red-500 dark:text-red-400">NASB:</span>
                <span>{entry.nasbTranslation}</span>
              </>
            )}

            {entry.wordOrigin && (
              <>
                <span className="font-semibold text-red-500 dark:text-red-400">{t("wordOriginLabel")}:</span>
                <span>{entry.wordOrigin}</span>
              </>
            )}

            <span className="font-semibold text-red-500 dark:text-red-400">Strong&apos;s:</span>
            <span className="font-mono">{entry.strongsNumber}</span>
          </div>
        </div>
      </div>

      {/* Strong's Exhaustive Concordance */}
      {entry.strongsExhaustive && (
        <div className="rounded-xl border border-border bg-card p-5 md:p-8">
          <h3 className="text-lg font-bold border-b border-border pb-2 mb-4">{t("strongsExhaustive")}</h3>
          <p className="text-sm leading-relaxed text-foreground/80">
            {cleanRefText(entry.strongsExhaustive)}
          </p>
        </div>
      )}

      {/* NAS Exhaustive Concordance */}
      {(entry.nasExhaustiveOrigin || entry.nasExhaustiveDefinition || entry.nasExhaustiveTranslation) && (
        <div className="rounded-xl border border-border bg-card p-5 md:p-8">
          <h3 className="text-lg font-bold border-b border-border pb-2 mb-4">{t("nasExhaustive")}</h3>
          <div className="space-y-3 text-sm">
            {entry.nasExhaustiveOrigin && (
              <div>
                <span className="font-semibold text-red-500 dark:text-red-400">{t("wordOriginLabel")}</span>
                <p className="mt-1 text-foreground/80">{cleanRefText(entry.nasExhaustiveOrigin)}</p>
              </div>
            )}
            {entry.nasExhaustiveDefinition && (
              <div>
                <span className="font-semibold text-red-500 dark:text-red-400">{t("definitionLabel")}</span>
                <p className="mt-1 text-foreground/80">{cleanRefText(entry.nasExhaustiveDefinition)}</p>
              </div>
            )}
            {entry.nasExhaustiveTranslation && (
              <div>
                <span className="font-semibold text-red-500 dark:text-red-400">NASB Translation</span>
                <p className="mt-1 text-foreground/80">{cleanRefText(entry.nasExhaustiveTranslation)}</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Full Definition (existing) */}
      {parsed && (
        <div className="rounded-xl border border-border bg-card p-5 md:p-8">
          <h3 className="text-lg font-bold border-b border-border pb-2 mb-4">{t("fullDefinition")}</h3>

          {parsed.intro && (
            <p className="text-sm leading-relaxed text-foreground/70 mb-4 italic">
              {cleanRefText(parsed.intro)}
            </p>
          )}

          {parsed.definitions.length > 0 ? (
            <div className="space-y-3">
              {parsed.definitions.map((def, idx) => (
                <div key={idx} className="flex gap-3">
                  <span className="font-bold text-primary text-sm min-w-[2rem] text-right shrink-0">
                    {def.label}
                  </span>
                  <p className="text-sm leading-relaxed">{cleanRefText(def.text)}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm leading-relaxed">
              {cleanRefText(entry.fullDefinition ?? "")}
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function formatPartOfSpeech(pos: string): string {
  const map: Record<string, string> = {
    "N-M": "Noun, Masculine", "N-F": "Noun, Feminine", "N-N": "Noun, Neuter",
    "N-M/F": "Noun, Masc/Fem", "V": "Verb", "ADJ": "Adjective", "ADV": "Adverb",
    "PREP": "Preposition", "CONJ": "Conjunction", "PRT": "Particle", "INJ": "Interjection",
    "T": "Article", "D": "Pronoun", "N-LI": "Letter/Indeclinable",
    "N-M-P": "Noun, Masc, Proper", "N-F-P": "Noun, Fem, Proper",
    "N-M-L": "Noun, Masc, Location", "N-F-L": "Noun, Fem, Location",
  };
  return map[pos] ?? pos;
}

function cleanRefText(text: string): string {
  return text.replace(/ref='[^']*'/g, "").replace(/<\/?[^>]+>/g, "").replace(/\s+/g, " ").trim();
}
