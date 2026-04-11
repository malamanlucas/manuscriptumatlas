export type CouncilType = "ECUMENICAL" | "REGIONAL" | "LOCAL";
export type DataConfidence = "HIGH" | "MEDIUM" | "LOW";

export interface CouncilSummaryDTO {
  id: number;
  displayName: string;
  slug: string;
  year: number;
  yearEnd?: number | null;
  century: number;
  councilType: CouncilType | string;
  location?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  numberOfParticipants?: number | null;
  consensusConfidence: number;
  dataConfidence: DataConfidence | string;
  sourceCount: number;
}

export interface CouncilFatherDTO {
  fatherId: number;
  fatherName: string;
  role?: string | null;
}

export interface CouncilHereticParticipantDTO {
  id: number;
  displayName: string;
  role?: string | null;
  description?: string | null;
}

export interface CouncilCanonDTO {
  id: number;
  canonNumber: number;
  title?: string | null;
  canonText: string;
  topic?: string | null;
}

export interface SourceClaimDTO {
  sourceDisplayName: string;
  sourceLevel: string;
  claimedYear?: number | null;
  claimedYearEnd?: number | null;
  claimedLocation?: string | null;
  claimedParticipants?: number | null;
  sourcePage?: string | null;
  rawText?: string | null;
}

export interface SourceDTO {
  id: number;
  name: string;
  displayName: string;
  sourceLevel: string;
  baseWeight: number;
  reliabilityScore?: number | null;
  url?: string | null;
  description?: string | null;
}

export interface HeresySummaryDTO {
  id: number;
  name: string;
  slug: string;
  centuryOrigin?: number | null;
  yearOrigin?: number | null;
  keyFigure?: string | null;
}

export interface CouncilDetailDTO extends CouncilSummaryDTO {
  shortDescription?: string | null;
  mainTopics?: string | null;
  keyParticipants?: string | null;
  originalText?: string | null;
  summary?: string | null;
  summaryReviewed: boolean;
  wikipediaUrl?: string | null;
  conflictResolution?: string | null;
  relatedFathers: CouncilFatherDTO[];
  hereticParticipants: CouncilHereticParticipantDTO[];
  heresies: HeresySummaryDTO[];
  canonCount: number;
  sourceClaims: SourceClaimDTO[];
}

export interface CouncilsListResponse {
  total: number;
  councils: CouncilSummaryDTO[];
}

export interface CouncilMapPointDTO {
  id: number;
  slug: string;
  displayName: string;
  year: number;
  councilType: string;
  latitude: number;
  longitude: number;
}

export interface CouncilTypeSummaryDTO {
  councilType: string;
  count: number;
}
