import type { CouncilSummaryDTO, HeresySummaryDTO } from './councils';

export interface HeresyDetailDTO extends HeresySummaryDTO {
  description?: string | null;
  wikipediaUrl?: string | null;
  councils: CouncilSummaryDTO[];
}

export interface HeresiesListResponse {
  total: number;
  heresies: HeresySummaryDTO[];
}
