export interface ChurchFatherSummary {
  id: number;
  displayName: string;
  normalizedName: string;
  centuryMin: number;
  centuryMax: number;
  tradition: string;
  primaryLocation: string | null;
  yearMin?: number | null;
  yearMax?: number | null;
  yearBest?: number | null;
  datingConfidence?: string | null;
  datingSource?: string | null;
  datingReference?: string | null;
}

export interface ChurchFatherDetail {
  id: number;
  displayName: string;
  normalizedName: string;
  centuryMin: number;
  centuryMax: number;
  shortDescription: string | null;
  primaryLocation: string | null;
  tradition: string;
  source: string;
  mannerOfDeath: string | null;
  biographySummary: string | null;
  biographyOriginal: string | null;
  biographyIsLong: boolean;
  yearMin?: number | null;
  yearMax?: number | null;
  yearBest?: number | null;
  datingSource?: string | null;
  datingReference?: string | null;
  datingConfidence?: string | null;
}

export interface ChurchFathersListResponse {
  total: number;
  fathers: ChurchFatherSummary[];
}

export type TextualTopic =
  | "MANUSCRIPTS"
  | "AUTOGRAPHS"
  | "APOCRYPHA"
  | "CANON"
  | "TEXTUAL_VARIANTS"
  | "TRANSLATION"
  | "CORRUPTION"
  | "SCRIPTURE_AUTHORITY";

export interface TextualStatementDTO {
  id: number;
  fatherId: number;
  fatherName: string;
  topic: TextualTopic;
  statementText: string;
  originalLanguage: string | null;
  originalText: string | null;
  sourceWork: string | null;
  sourceReference: string | null;
  approximateYear: number | null;
}

export interface TextualStatementsListResponse {
  total: number;
  statements: TextualStatementDTO[];
}

export interface TopicSummaryDTO {
  topic: string;
  count: number;
}

export interface TopicsSummaryResponse {
  topics: TopicSummaryDTO[];
}

export interface PatristicStatsDTO {
  totalFathers: number;
  totalStatements: number;
  totalWithDating: number;
  byTradition: Record<string, number>;
  byCentury: Record<string, number>;
  topicsSummary: TopicSummaryDTO[];
}
