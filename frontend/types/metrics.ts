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
