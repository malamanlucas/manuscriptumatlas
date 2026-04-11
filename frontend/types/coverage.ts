import type { MissingVerse, ChapterCoverage } from './common';

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
