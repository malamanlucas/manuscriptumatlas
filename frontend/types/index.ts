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
  heresies: HeresySummaryDTO[];
  canonCount: number;
  sourceClaims: SourceClaimDTO[];
}

export interface CouncilsListResponse {
  total: number;
  councils: CouncilSummaryDTO[];
}

export interface HeresyDetailDTO extends HeresySummaryDTO {
  description?: string | null;
  wikipediaUrl?: string | null;
  councils: CouncilSummaryDTO[];
}

export interface HeresiesListResponse {
  total: number;
  heresies: HeresySummaryDTO[];
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

export interface PhaseStatusDTO {
  phaseName: string;
  status: "idle" | "running" | "success" | "failed" | string;
  startedAt?: string | null;
  completedAt?: string | null;
  itemsProcessed: number;
  itemsTotal: number;
  errorMessage?: string | null;
  lastRunBy?: string | null;
}

export interface CacheEntryDTO {
  key: string;
  sizeBytes: number;
}

export interface CacheStatsDTO {
  totalFiles: number;
  totalSizeBytes: number;
  totalSizeMb: number;
  entries: CacheEntryDTO[];
}

// ── Visitor Tracking Types ──

export interface AnalyticsOverview {
  activeNow: number;
  sessionsInRange: number;
  uniqueVisitorsInRange: number;
  pageviewsInRange: number;
  avgLoadTimeMs: number | null;
}

export interface VisitorSessionCompact {
  id: number;
  visitorId: string;
  sessionId: string;
  ipAddress: string;
  browserName: string | null;
  browserVersion: string | null;
  osName: string | null;
  deviceType: string | null;
  language: string | null;
  timezone: string | null;
  referrer: string | null;
  pageLoadTimeMs: number | null;
  createdAt: string;
  lastActivityAt: string;
}

export interface VisitorSessionComplete extends VisitorSessionCompact {
  userAgent: string;
  osVersion: string | null;
  screenWidth: number | null;
  screenHeight: number | null;
  viewportWidth: number | null;
  viewportHeight: number | null;
  languages: string | null;
  platform: string | null;
  networkInfo: string | null;
  deviceMemory: number | null;
  hardwareConcurrency: number | null;
  colorDepth: number | null;
  pixelRatio: number | null;
  touchPoints: number | null;
  cookieEnabled: boolean | null;
  doNotTrack: boolean | null;
  webglRenderer: string | null;
  webglVendor: string | null;
  canvasFingerprint: string | null;
}

export interface SessionsPageResponse {
  total: number;
  page: number;
  limit: number;
  sessions: VisitorSessionCompact[];
}

export interface SessionsPageCompleteResponse {
  total: number;
  page: number;
  limit: number;
  sessions: VisitorSessionComplete[];
}

export interface LiveVisitorDTO {
  sessionId: string;
  visitorId: string;
  ipAddress: string;
  browserName: string | null;
  osName: string | null;
  deviceType: string | null;
  currentPage: string | null;
  sessionStarted: string;
  lastActivity: string;
}

export interface FilterValuesResponse {
  browsers: string[];
  operatingSystems: string[];
  deviceTypes: string[];
  languages: string[];
  timezones: string[];
  connectionTypes: string[];
  paths: string[];
}

export interface TimelineBucket {
  bucket: string;
  count: number;
  series?: Record<string, number>;
}

export interface TimelineAnalyticsResponse {
  granularity: string;
  breakdown: string;
  buckets: TimelineBucket[];
}

export interface HeatmapCell {
  dayOfWeek: number;
  hourOfDay: number;
  count: number;
}

export interface HeatmapResponse {
  cells: HeatmapCell[];
}

export interface VisitorSummaryDTO {
  visitorId: string;
  sessionCount: number;
  totalPageviews: number;
  firstSeenAt: string;
  lastSeenAt: string;
  lastBrowser: string | null;
  lastOs: string | null;
  lastDeviceType: string | null;
  lastIp: string | null;
}

export interface VisitorsListResponse {
  total: number;
  page: number;
  limit: number;
  visitors: VisitorSummaryDTO[];
}

export interface DistributionItem {
  value: string;
  count: number;
  percent: number;
}

export interface DistributionResponse {
  field: string;
  total: number;
  items: DistributionItem[];
}

export interface TopPageDTO {
  path: string;
  count: number;
  avgDurationMs: number | null;
}

export interface TopReferrerDTO {
  referrer: string;
  count: number;
}

export interface DailyStatDTO {
  date: string;
  totalSessions: number;
  totalPageviews: number;
  uniqueVisitors: number;
  avgSessionDurationMs: number | null;
  topBrowsers: string | null;
  topOs: string | null;
  topDevices: string | null;
  topPages: string | null;
}

export interface TrendsResponse {
  days: DailyStatDTO[];
}

export interface PageViewDTO {
  id: number;
  sessionId: string;
  visitorId: string;
  path: string;
  referrerPath: string | null;
  durationMs: number | null;
  createdAt: string;
}

// ── Auth / User Management ──

export interface UserDTO {
  id: number;
  email: string;
  displayName: string;
  pictureUrl: string | null;
  role: "ADMIN" | "MEMBER";
}

export interface SessionFilters {
  from?: string;
  to?: string;
  days?: number;
  browser?: string;
  os?: string;
  deviceType?: string;
  language?: string;
  timezone?: string;
  ip?: string;
  visitorId?: string;
  fingerprint?: string;
  referrer?: string;
  returning?: boolean;
  minLoadTime?: number;
  maxLoadTime?: number;
  sort?: string;
  order?: string;
  page?: number;
  limit?: number;
  view?: "compact" | "complete";
}
