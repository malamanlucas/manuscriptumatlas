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
  yearMin?: number | null;
  yearMax?: number | null;
  yearBest?: number | null;
  datingConfidence?: string | null;
  datingSource?: string | null;
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
  yearMin?: number | null;
  yearMax?: number | null;
  yearBest?: number | null;
  datingSource?: string | null;
  datingReference?: string | null;
  datingConfidence?: string | null;
}

export interface VerseManuscriptItem {
  gaId: string;
  name: string | null;
  centuryMin: number;
  centuryMax: number;
  type: string | null;
  ntvmrUrl: string | null;
}

export interface VerseManuscriptsResponse {
  book: string;
  chapter: number;
  verse: number;
  manuscripts: VerseManuscriptItem[];
}
