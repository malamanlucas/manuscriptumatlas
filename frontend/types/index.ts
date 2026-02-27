export interface BookCoverage {
  bookName: string;
  coveredVerses: number;
  totalVerses: number;
  coveragePercent: number;
}

export interface CoverageSummary {
  totalNtVerses: number;
  coveredVerses: number;
  overallCoveragePercent: number;
}

export interface CenturyCoverageResponse {
  century: number;
  summary: CoverageSummary;
  books: BookCoverage[];
  fullyAttested: string[];
  notFullyAttested: BookCoverage[];
}

export interface FullCoverageResponse {
  maxCentury: number;
  summary: CoverageSummary;
  byCentury: CenturyCoverageResponse[];
}

export interface ChapterCoverage {
  chapter: number;
  coveredVerses: number;
  totalVerses: number;
  coveragePercent: number;
  coveredList: number[];
  missingList: number[];
}

export interface BookChapterCoverageResponse {
  book: string;
  century: number;
  chapters: ChapterCoverage[];
}

export interface GospelCoverageResponse {
  century: number;
  individual: BookCoverage[];
  aggregated: CoverageSummary;
  missingVerses: MissingVerse[];
}

export interface TimelineEntry {
  century: number;
  cumulativePercent: number;
  coveredVerses: number;
  newVersesCount: number;
  missingVersesCount: number;
  growthPercent: number;
}

export interface TimelineResponse {
  book: string | null;
  type: string | null;
  totalVerses: number;
  entries: TimelineEntry[];
}

export interface MissingVerse {
  book: string;
  chapter: number;
  verse: number;
}

export interface MissingVersesResponse {
  book: string;
  century: number;
  totalMissing: number;
  missingVerses: MissingVerse[];
}

export interface StatsOverviewResponse {
  totalManuscripts: number;
  byType: Record<string, number>;
  byCentury: { century: number; count: number }[];
  byBook: { bookName: string; manuscriptCount: number }[];
  avgBooksPerManuscript: number;
  totalVerses: number;
  coveredVerses: number;
  overallCoveragePercent: number;
}

export interface ManuscriptSummary {
  gaId: string;
  name: string | null;
  centuryMin: number;
  centuryMax: number;
  manuscriptType: string | null;
  bookCount: number;
  verseCount: number;
}

export interface BookRanges {
  book: string;
  ranges: string[];
}

export interface BookInterval {
  book: string;
  chapterMin: number;
  chapterMax: number;
  verseCount: number;
}

export interface ManuscriptDetailResponse {
  gaId: string;
  name: string | null;
  centuryMin: number;
  centuryMax: number;
  manuscriptType: string | null;
  booksPreserved: BookRanges[];
  intervals: BookInterval[];
  dataSource: string;
  ntvmrUrl: string;
  historicalNotes?: string | null;
}

export interface CenturyGrowthRate {
  century: number;
  rate: number;
}

export interface CenturyCoveragePercent {
  century: number;
  percent: number;
}

export interface BookMetricsResponse {
  bookName: string;
  centuryGrowthRates: CenturyGrowthRate[];
  stabilizationCentury: number | null;
  fragmentationIndex: number;
  coverageDensity: number;
  manuscriptConcentrationScore: number;
  coverageByCentury: CenturyCoveragePercent[];
}

export interface NtMetricsResponse {
  books: BookMetricsResponse[];
  overallStabilizationCentury: number | null;
  overallCoverageByCentury: CenturyCoveragePercent[];
}

export interface ManuscriptsCountResponse {
  total: number;
  papyrus: number;
  uncial: number;
  minuscule: number;
  lectionary: number;
}

export interface IngestionStatusResponse {
  status: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
  manuscriptsIngested: number;
  versesLinked: number;
  errorMessage: string | null;
  isRunning: boolean;
  enableIngestion: boolean;
}
