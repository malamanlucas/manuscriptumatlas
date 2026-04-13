"use client";

import { useState, useCallback } from "react";
import { useTranslations } from "next-intl";
import { Loader2, ChevronDown, ChevronLeft, ChevronRight } from "lucide-react";
import Link from "next/link";
import type { BibleVerseTextDTO, InterlinearWordDTO } from "@/types";

interface InterlinearData {
  words: InterlinearWordDTO[];
  kjvText?: string | null;
}

interface CompareText {
  version: string;
  textByVerse: Map<number, string>;
}

interface LinkedVerseReaderProps {
  primaryVersion: string;
  primaryVerses: BibleVerseTextDTO[];
  interlinearByVerse: Map<number, InterlinearData>;
  compareTexts: CompareText[];
  book: string;
  chapter: number;
  totalChapters?: number;
  isLoading: boolean;
  error: Error | null;
  interlinearMode: boolean;
  alignVersion?: string;
  onPrevChapter?: () => void;
  onNextChapter?: () => void;
}

const VERSION_COLORS: Record<string, string> = {
  KJV: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
  AA: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
  ACF: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
  ARC: "bg-purple-500/10 text-purple-600 dark:text-purple-400",
};

function getVersionColor(code: string): string {
  return VERSION_COLORS[code] ?? "bg-muted text-muted-foreground";
}

/** Maps semanticRelation to gloss link text color */
function getSemanticGlossColor(relation?: string | null, isDivergentFallback?: boolean): string {
  if (relation) {
    switch (relation) {
      case "equivalent":
      case "synonymous":
        return "text-emerald-600 dark:text-emerald-400";
      case "related":
        return "text-amber-600 dark:text-amber-400";
      case "divergent":
        return "text-red-500 dark:text-red-400";
      default:
        break;
    }
  }
  // Fallback for legacy data without semanticRelation
  return isDivergentFallback
    ? "text-amber-600 dark:text-amber-400"
    : "text-blue-500 dark:text-blue-400";
}

/** Maps semanticRelation to aligned text annotation color */
function getSemanticAlignedColor(relation?: string | null): string {
  switch (relation) {
    case "synonymous":
      return "text-emerald-600/70 dark:text-emerald-400/70";
    case "related":
      return "text-amber-600/70 dark:text-amber-400/70";
    case "divergent":
      return "text-red-500/70 dark:text-red-400/70";
    default:
      return "text-amber-600/70 dark:text-amber-400/70";
  }
}

export function LinkedVerseReader({
  primaryVersion,
  primaryVerses,
  interlinearByVerse,
  compareTexts,
  book,
  chapter,
  totalChapters,
  isLoading,
  error,
  interlinearMode,
  alignVersion = "KJV",
  onPrevChapter,
  onNextChapter,
}: LinkedVerseReaderProps) {
  const t = useTranslations("bible");
  const tc = useTranslations("common");
  const [expandedVerse, setExpandedVerse] = useState<number | null>(null);
  const [hoveredGreekPos, setHoveredGreekPos] = useState<{ verse: number; pos: number } | null>(null);
  const [hoveredKjvIdx, setHoveredKjvIdx] = useState<{ verse: number; idx: number } | null>(null);

  if (isLoading) {
    return (
      <div className="rounded-xl border border-border bg-card p-16 text-center">
        <Loader2 className="mx-auto h-6 w-6 animate-spin text-muted-foreground" />
        <p className="mt-3 text-sm text-muted-foreground">{tc("loading")}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-300/50 bg-red-50 p-6 text-red-700 dark:border-red-800/50 dark:bg-red-950/30 dark:text-red-300">
        {tc("failedToLoad", { error: error.message })}
      </div>
    );
  }

  if (primaryVerses.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-16 text-center text-muted-foreground">
        {t("noVerses")}
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-border bg-card">
      {/* Chapter header */}
      <div className="border-b border-border px-5 py-4 md:px-8">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold md:text-2xl">
            {book} {chapter}
          </h2>
          <span className={`rounded-full px-3 py-1 text-xs font-bold ${getVersionColor(primaryVersion)}`}>
            {primaryVersion}
          </span>
        </div>
      </div>

      {/* Verses */}
      <div className="divide-y divide-border/30 px-5 md:px-8">
        {primaryVerses.map((verse) => {
          const interlinearData = interlinearByVerse.get(verse.verseNumber);
          const hasInterlinear = interlinearData && interlinearData.words.length > 0;
          const hasAlignment = hasInterlinear && interlinearData.words.some((w) => w.kjvAlignment);
          const isExpanded = expandedVerse === verse.verseNumber;
          const showInterlinear = interlinearMode || isExpanded;

          return (
            <div key={verse.verseNumber} className="py-4 md:py-5">
              {/* Main verse text */}
              <div className="flex items-start gap-3">
                <span className="mt-0.5 text-sm font-bold text-primary select-none min-w-[1.5rem] text-right shrink-0">
                  {verse.verseNumber}
                </span>
                <div className="flex-1 min-w-0">
                  {/* Primary version text with alignment highlighting */}
                  <p className="text-base leading-relaxed md:text-lg md:leading-loose">
                    {hasAlignment && interlinearData.kjvText ? (
                      <KjvTextWithHighlight
                        text={verse.text}
                        words={interlinearData.words}
                        verseNumber={verse.verseNumber}
                        hoveredGreekPos={hoveredGreekPos}
                        hoveredKjvIdx={hoveredKjvIdx}
                        onHoverKjvIdx={(idx) =>
                          setHoveredKjvIdx(idx !== null ? { verse: verse.verseNumber, idx } : null)
                        }
                      />
                    ) : hasInterlinear ? (
                      <LinkedText text={verse.text} words={interlinearData.words} />
                    ) : (
                      <span>{verse.text}</span>
                    )}
                  </p>

                  {/* Compare versions */}
                  {compareTexts.length > 0 && (
                    <div className="mt-3 space-y-2 border-l-2 border-border/50 pl-4">
                      {compareTexts.map((ct) => {
                        const compareText = ct.textByVerse.get(verse.verseNumber);
                        if (!compareText) return null;
                        return (
                          <div key={ct.version} className="flex items-start gap-2">
                            <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold ${getVersionColor(ct.version)}`}>
                              {ct.version}
                            </span>
                            <p className="text-sm leading-relaxed text-foreground/70">
                              {compareText}
                            </p>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {/* Expand interlinear toggle (only when not in interlinear mode) */}
                  {hasInterlinear && !interlinearMode && (
                    <button
                      onClick={() => setExpandedVerse(isExpanded ? null : verse.verseNumber)}
                      className="mt-2 flex items-center gap-1.5 text-xs text-primary/50 hover:text-primary transition-colors"
                    >
                      <ChevronDown className={`h-3.5 w-3.5 transition-transform duration-200 ${isExpanded ? "rotate-180" : ""}`} />
                      {isExpanded ? t("hideInterlinear") : t("showInterlinear")}
                    </button>
                  )}

                  {/* Interlinear word cards */}
                  {showInterlinear && hasInterlinear && (
                    <div className="mt-3 overflow-x-auto pb-2">
                      <div className="flex flex-wrap gap-1.5 min-w-0">
                        {interlinearData.words.map((w, idx) => {
                          const alignment = w.kjvAlignment;
                          const isGreekHovered =
                            hoveredGreekPos?.verse === verse.verseNumber &&
                            hoveredGreekPos?.pos === w.wordPosition;
                          const isKjvHovered =
                            hoveredKjvIdx?.verse === verse.verseNumber &&
                            alignment?.kjvIndices?.includes(hoveredKjvIdx.idx);

                          const isHovered = isGreekHovered || isKjvHovered;

                          let borderColor = "border-border/40";
                          let bgColor = "bg-muted/15";
                          if (alignment && alignment.alignedText) {
                            const rel = alignment.semanticRelation ?? "unknown";
                            if (rel === "equivalent" || rel === "synonymous") {
                              borderColor = "border-emerald-500/30";
                              bgColor = "bg-emerald-500/5";
                            } else if (rel === "related") {
                              borderColor = "border-amber-500/30";
                              bgColor = "bg-amber-500/5";
                            } else if (rel === "divergent") {
                              borderColor = "border-red-500/30";
                              bgColor = "bg-red-500/5";
                            } else {
                              // legacy / unknown — fallback to confidence-based
                              const conf = alignment.confidence ?? 0;
                              if (conf >= 80) {
                                borderColor = "border-blue-500/30";
                                bgColor = "bg-blue-500/5";
                              } else if (conf >= 60) {
                                borderColor = "border-blue-400/20";
                                bgColor = "bg-blue-400/5";
                              } else {
                                borderColor = "border-amber-500/40";
                                bgColor = "bg-amber-500/10";
                              }
                            }
                          }
                          if (isHovered) {
                            borderColor = "border-primary";
                            bgColor = "bg-primary/15";
                          }

                          const hasMapping = alignment?.alignedText;

                          return (
                            <div
                              key={idx}
                              className={`flex flex-col items-center rounded-lg border ${borderColor} ${bgColor} px-2.5 py-2 text-center min-w-[65px] transition-colors duration-150 ${
                                hasMapping ? "cursor-pointer" : "cursor-default"
                              } ${
                                isHovered ? "outline outline-2 outline-primary/50" : ""
                              }`}
                              onMouseEnter={() =>
                                setHoveredGreekPos({ verse: verse.verseNumber, pos: w.wordPosition })
                              }
                              onMouseLeave={() => setHoveredGreekPos(null)}
                            >
                              {/* Greek word */}
                              <span className="text-sm font-semibold text-primary leading-tight">
                                {w.originalWord}
                              </span>

                              {/* Transliteration */}
                              {w.transliteration && (
                                <span className="text-[10px] italic text-muted-foreground/60 leading-tight">
                                  {w.transliteration}
                                </span>
                              )}

                              {/* Raw gloss */}
                              {w.strongsNumber ? (
                                <Link
                                  href={`/bible/strongs/${w.strongsNumber}`}
                                  className={`mt-1 text-xs font-medium leading-tight hover:underline ${
                                    getSemanticGlossColor(alignment?.semanticRelation, alignment?.isDivergent)
                                  }`}
                                >
                                  {w.englishGloss || "-"}
                                </Link>
                              ) : (
                                <span className="mt-1 text-xs text-muted-foreground leading-tight">
                                  {w.englishGloss || "-"}
                                </span>
                              )}

                              {/* Aligned text + contextual sense (shown when not equivalent) */}
                              {alignment?.alignedText && (alignment.semanticRelation ? alignment.semanticRelation !== "equivalent" : alignment.isDivergent) && (
                                <span className={`mt-0.5 text-[10px] leading-tight ${getSemanticAlignedColor(alignment.semanticRelation)}`}>
                                  {alignVersion}: {alignment.alignedText}
                                  {alignment.contextualSense && alignment.contextualSense !== alignment.alignedText && (
                                    <> ({alignment.contextualSense})</>
                                  )}
                                </span>
                              )}

                              {/* Strong's number */}
                              {w.strongsNumber && (
                                <Link
                                  href={`/bible/strongs/${w.strongsNumber}`}
                                  className="mt-0.5 text-[10px] text-muted-foreground/60 font-mono hover:text-primary transition-colors"
                                >
                                  {w.strongsNumber}
                                </Link>
                              )}
                            </div>
                          );
                        })}
                      </div>
                      <p className="mt-1 text-[10px] text-muted-foreground/40 md:hidden">← scroll →</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Chapter navigation footer */}
      <div className="border-t border-border px-5 py-4 md:px-8">
        <div className="flex items-center justify-between">
          <button
            onClick={onPrevChapter}
            disabled={!onPrevChapter || chapter <= 1}
            className="flex items-center gap-1.5 rounded-lg border border-border px-4 py-2 text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="h-4 w-4" />
            {t("prevChapter")}
          </button>
          <span className="text-xs text-muted-foreground">
            {book} {chapter}{totalChapters ? ` / ${totalChapters}` : ""}
          </span>
          <button
            onClick={onNextChapter}
            disabled={!onNextChapter || (totalChapters !== undefined && chapter >= totalChapters)}
            className="flex items-center gap-1.5 rounded-lg border border-border px-4 py-2 text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            {t("nextChapter")}
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

// ── KJV text with bidirectional hover highlighting ──

function KjvTextWithHighlight({
  text,
  words,
  verseNumber,
  hoveredGreekPos,
  hoveredKjvIdx,
  onHoverKjvIdx,
}: {
  text: string;
  words: InterlinearWordDTO[];
  verseNumber: number;
  hoveredGreekPos: { verse: number; pos: number } | null;
  hoveredKjvIdx: { verse: number; idx: number } | null;
  onHoverKjvIdx: (idx: number | null) => void;
}) {
  // Build reverse mapping: KJV word index → Greek word(s) data
  const kjvIdxToGreek = new Map<number, InterlinearWordDTO[]>();
  for (const w of words) {
    if (w.kjvAlignment?.kjvIndices) {
      for (const idx of w.kjvAlignment.kjvIndices) {
        const existing = kjvIdxToGreek.get(idx) || [];
        existing.push(w);
        kjvIdxToGreek.set(idx, existing);
      }
    }
  }

  // Find which KJV indices should be highlighted
  const highlightedKjvIndices = new Set<number>();

  // From hovered Greek word → highlight its KJV indices
  if (hoveredGreekPos?.verse === verseNumber) {
    const greekWord = words.find((w) => w.wordPosition === hoveredGreekPos.pos);
    if (greekWord?.kjvAlignment?.kjvIndices) {
      for (const idx of greekWord.kjvAlignment.kjvIndices) {
        highlightedKjvIndices.add(idx);
      }
    }
  }

  // From hovered KJV word → highlight ALL KJV words in the same expression group
  if (hoveredKjvIdx?.verse === verseNumber) {
    const greekWordsForHoveredKjv = kjvIdxToGreek.get(hoveredKjvIdx.idx);
    if (greekWordsForHoveredKjv) {
      for (const gw of greekWordsForHoveredKjv) {
        if (gw.kjvAlignment?.kjvIndices) {
          for (const idx of gw.kjvAlignment.kjvIndices) {
            highlightedKjvIndices.add(idx);
          }
        }
      }
    }
  }

  const segments = text.split(/(\s+)/);
  let wordIdx = 0;

  return (
    <span>
      {segments.map((segment, i) => {
        if (/^\s+$/.test(segment)) return <span key={i}>{segment}</span>;

        const idx = wordIdx++;
        const greekWords = kjvIdxToGreek.get(idx);
        const isHighlighted = highlightedKjvIndices.has(idx);
        const hasNonEquivalent = greekWords?.some((w) => {
          const rel = w.kjvAlignment?.semanticRelation;
          return rel ? rel !== "equivalent" && rel !== "synonymous" : w.kjvAlignment?.isDivergent;
        });
        const hasMapping = greekWords && greekWords.length > 0;

        const strongsNumber = greekWords?.[0]?.strongsNumber;

        let className = "transition-colors duration-150";
        if (isHighlighted) {
          className += hasNonEquivalent
            ? " bg-amber-500/25 text-amber-800 dark:text-amber-300 rounded-sm"
            : " bg-primary/20 text-primary rounded-sm";
        } else if (hasMapping) {
          className += " text-foreground decoration-primary/30 underline underline-offset-4 decoration-dotted";
        } else {
          className += " text-muted-foreground/60";
        }

        if (hasMapping && strongsNumber) {
          return (
            <Link
              key={i}
              href={`/bible/strongs/${strongsNumber}`}
              className={className}
              onMouseEnter={() => onHoverKjvIdx(idx)}
              onMouseLeave={() => onHoverKjvIdx(null)}
            >
              {segment}
            </Link>
          );
        }

        return (
          <span
            key={i}
            className={className}
            onMouseEnter={() => hasMapping && onHoverKjvIdx(idx)}
            onMouseLeave={() => onHoverKjvIdx(null)}
          >
            {segment}
          </span>
        );
      })}
    </span>
  );
}

// ── LinkedText: fallback rendering with clickable Strong's links ──

function LinkedText({ text, words }: { text: string; words: InterlinearWordDTO[] }) {
  const glossToStrongs = new Map<string, string>();
  for (const w of words) {
    if (w.strongsNumber && w.englishGloss) {
      const cleanGloss = w.englishGloss.replace(/[[\]<>{}(),;.!?]/g, "").trim().toLowerCase();
      if (cleanGloss) {
        for (const part of cleanGloss.split(/\s+/)) {
          if (part.length > 1 && !glossToStrongs.has(part)) {
            glossToStrongs.set(part, w.strongsNumber);
          }
        }
      }
    }
  }

  const segments = text.split(/(\s+)/);

  return (
    <span>
      {segments.map((segment, idx) => {
        if (/^\s+$/.test(segment)) return <span key={idx}>{segment}</span>;

        const cleanWord = segment.replace(/[.,;:!?"'()\[\]{}]/g, "").toLowerCase();
        const strongs = glossToStrongs.get(cleanWord);

        if (strongs) {
          return (
            <Link
              key={idx}
              href={`/bible/strongs/${strongs}`}
              className="text-blue-500 dark:text-blue-400 hover:underline decoration-dotted underline-offset-2"
              title={`Strong's ${strongs}`}
            >
              {segment}
            </Link>
          );
        }

        return <span key={idx}>{segment}</span>;
      })}
    </span>
  );
}
