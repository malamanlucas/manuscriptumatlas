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
