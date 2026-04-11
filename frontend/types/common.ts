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

export interface ChapterCoverage {
  chapter: number;
  coveredVerses: number;
  totalVerses: number;
  coveragePercent: number;
  coveredList: number[];
  missingList: number[];
}
