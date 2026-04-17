// ── Bible ──

export interface BibleVersionDTO {
  id: number;
  code: string;
  name: string;
  language: string;
  description?: string | null;
  isPrimary: boolean;
  testamentScope: string;
}

export interface BibleBookDTO {
  id: number;
  name: string;
  abbreviation: string;
  totalChapters: number;
  totalVerses: number;
  bookOrder: number;
  testament: string;
  abbreviations: Record<string, string[]>;
}

export interface BibleVerseTextDTO {
  verseNumber: number;
  text: string;
}

export interface BibleChapterResponse {
  version: string;
  book: string;
  bookId: number;
  chapter: number;
  totalVerses: number;
  verses: BibleVerseTextDTO[];
}

export interface ResolvedReferenceDTO {
  book: string;
  bookId: number;
  chapter: number;
  verseStart?: number | null;
  verseEnd?: number | null;
  isChapterOnly: boolean;
}

export interface WordAlignmentDTO {
  wordPosition: number;
  kjvIndices: number[] | null;
  alignedText: string | null;
  isDivergent: boolean;
  confidence: number;
  tokenPositions?: number[] | null;
  method?: string | null;
  contextualSense?: string | null;
  semanticRelation?: string | null;
}

export interface InterlinearWordDTO {
  wordPosition: number;
  originalWord: string;
  transliteration?: string | null;
  lemma: string;
  morphology?: string | null;
  strongsNumber?: string | null;
  englishGloss?: string | null;
  portugueseGloss?: string | null;
  spanishGloss?: string | null;
  language: string;
  kjvAlignment?: WordAlignmentDTO | null;
}

export interface InterlinearVerseDTO {
  book: string;
  chapter: number;
  verseNumber: number;
  words: InterlinearWordDTO[];
  kjvText?: string | null;
}

export interface InterlinearChapterDTO {
  book: string;
  chapter: number;
  verses: InterlinearVerseDTO[];
}

export interface LexiconEntryDTO {
  id: number;
  strongsNumber: string;
  lemma: string;
  transliteration?: string | null;
  pronunciation?: string | null;
  shortDefinition?: string | null;
  fullDefinition?: string | null;
  partOfSpeech?: string | null;
  usageCount: number;
  language: string;
  phoneticSpelling?: string | null;
  kjvTranslation?: string | null;
  kjvUsageCount?: number | null;
  nasbTranslation?: string | null;
  wordOrigin?: string | null;
  strongsExhaustive?: string | null;
  nasExhaustiveOrigin?: string | null;
  nasExhaustiveDefinition?: string | null;
  nasExhaustiveTranslation?: string | null;
}

export interface StrongsOccurrenceDTO {
  book: string;
  bookOrder: number;
  chapter: number;
  verseNumber: number;
  originalWord: string;
  lemma: string;
  morphology?: string | null;
}

export interface StrongsConcordanceDTO {
  strongsNumber: string;
  lexiconEntry?: LexiconEntryDTO | null;
  totalOccurrences: number;
  occurrences: StrongsOccurrenceDTO[];
  page: number;
  limit: number;
}

export interface BibleCompareRow {
  verseNumber: number;
  texts: Record<string, string>;
}

export interface BibleCompareResponse {
  book: string;
  chapter: number;
  verseNumber?: number | null;
  versions: string[];
  verses: BibleCompareRow[];
}

export interface BibleSearchResultDTO {
  book: string;
  chapter: number;
  verseNumber: number;
  version: string;
  text: string;
  snippet?: string | null;
}

export interface BibleSearchResponse {
  query: string;
  totalResults: number;
  results: BibleSearchResultDTO[];
  page: number;
  limit: number;
  isReference: boolean;
  resolvedReference?: ResolvedReferenceDTO | null;
}

// ── Layer 4 scope/coverage/history ──

export interface RunScopedRequest {
  phases: string[];
  bookName?: string | null;
  chapter?: number | null;
  verse?: number | null;
}

export interface RunScopedResponse {
  message: string;
  applicationIds: number[];
}

export interface BibleLayer4VerseCoverageDTO {
  verse: number;
  tokenizeArc69: boolean;
  tokenizeKjv: boolean;
  alignKjv: boolean;
  alignArc69: boolean;
  enrichSemanticsArc69: boolean;
}

export interface BibleLayer4CoverageDTO {
  book: string;
  chapter: number;
  totalVerses: number;
  verses: BibleLayer4VerseCoverageDTO[];
}

export interface BibleLayer4ApplicationDTO {
  id: number;
  phaseName: string;
  bookName: string | null;
  chapter: number | null;
  verse: number | null;
  status: string;
  itemsProcessed: number;
  enqueuedCount: number;
  errorMessage: string | null;
  requestedAt: string;
  finishedAt: string | null;
}
