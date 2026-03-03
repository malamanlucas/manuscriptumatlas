import type {
  CenturyCoverageResponse,
  FullCoverageResponse,
  BookChapterCoverageResponse,
  GospelCoverageResponse,
  TimelineResponse,
  MissingVersesResponse,
  StatsOverviewResponse,
  ManuscriptSummary,
  ManuscriptDetailResponse,
  BookMetricsResponse,
  NtMetricsResponse,
  ManuscriptsCountResponse,
  IngestionStatusResponse,
  VerseManuscriptsResponse,
  ChurchFathersListResponse,
  ChurchFatherDetail,
  ChurchFatherSummary,
  TextualStatementsListResponse,
  TextualStatementDTO,
  TopicsSummaryResponse,
} from "@/types";

const BASE = "/api";

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

function buildParams(params: Record<string, string | undefined>): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== "") sp.set(k, v);
  }
  const str = sp.toString();
  return str ? `?${str}` : "";
}

export function getCoverage(
  century?: number,
  type?: string
): Promise<FullCoverageResponse | CenturyCoverageResponse> {
  const params = buildParams({
    century: century?.toString(),
    type,
  });
  return fetchJson(`${BASE}/coverage${params}`);
}

export function getCoverageByCentury(
  century: number,
  type?: string
): Promise<CenturyCoverageResponse> {
  const params = buildParams({ type });
  return fetchJson(`${BASE}/century/${century}${params}`);
}

export function getChapterCoverage(
  book: string,
  century: number,
  type?: string
): Promise<BookChapterCoverageResponse> {
  const params = buildParams({ type });
  return fetchJson(
    `${BASE}/coverage/${encodeURIComponent(book)}/chapters/${century}${params}`
  );
}

export function getGospelCoverage(
  century: number,
  type?: string
): Promise<GospelCoverageResponse> {
  const params = buildParams({ type });
  return fetchJson(`${BASE}/coverage/gospels/${century}${params}`);
}

export function getTimeline(
  book?: string,
  type?: string
): Promise<TimelineResponse> {
  const params = buildParams({ book, type });
  return fetchJson(`${BASE}/timeline${params}`);
}

export function getTimelineFull(type?: string): Promise<TimelineResponse> {
  const params = buildParams({ type });
  return fetchJson(`${BASE}/timeline/full${params}`);
}

export function getMissingVerses(
  book: string,
  century: number,
  type?: string
): Promise<MissingVersesResponse> {
  const params = buildParams({ type });
  return fetchJson(
    `${BASE}/missing/${encodeURIComponent(book)}/${century}${params}`
  );
}

export function getStatsOverview(): Promise<StatsOverviewResponse> {
  return fetchJson(`${BASE}/stats/overview`);
}

export function getManuscripts(params?: {
  type?: string;
  century?: number;
  page?: number;
  limit?: number;
}): Promise<ManuscriptSummary[]> {
  const q = buildParams({
    type: params?.type,
    century: params?.century?.toString(),
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
  });
  return fetchJson(`${BASE}/manuscripts${q}`);
}

export function getManuscriptDetail(gaId: string): Promise<ManuscriptDetailResponse> {
  return fetchJson(`${BASE}/manuscripts/${encodeURIComponent(gaId)}`);
}

export function getManuscriptsForVerse(
  book: string,
  chapter: number,
  verse: number,
  type?: string
): Promise<VerseManuscriptsResponse> {
  const params = buildParams({
    book,
    chapter: chapter.toString(),
    verse: verse.toString(),
    type,
  });
  return fetchJson(`${BASE}/verses/manuscripts${params}`);
}

export function getNtMetrics(): Promise<NtMetricsResponse> {
  return fetchJson(`${BASE}/metrics/nt`);
}

export function getBookMetrics(book: string): Promise<BookMetricsResponse> {
  return fetchJson(`${BASE}/metrics/${encodeURIComponent(book)}`);
}

export function getManuscriptsCount(): Promise<ManuscriptsCountResponse> {
  return fetchJson(`${BASE}/stats/manuscripts-count`);
}

export function getIngestionStatus(): Promise<IngestionStatusResponse> {
  return fetchJson(`${BASE}/admin/ingestion/status`);
}

export async function triggerIngestion(): Promise<{ message: string }> {
  const res = await fetch(`${BASE}/admin/ingestion/run`, { method: "POST" });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

export async function resetAndReIngest(): Promise<{ message: string }> {
  const res = await fetch(`${BASE}/admin/ingestion/reset`, { method: "POST" });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

export function getChurchFathers(params?: {
  century?: number;
  tradition?: string;
  page?: number;
  limit?: number;
  locale?: string;
}): Promise<ChurchFathersListResponse> {
  const q = buildParams({
    century: params?.century?.toString(),
    tradition: params?.tradition,
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    locale: params?.locale,
  });
  return fetchJson(`${BASE}/fathers${q}`);
}

export function getChurchFatherDetail(
  id: number,
  locale?: string
): Promise<ChurchFatherDetail> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/fathers/${id}${q}`);
}

export function searchChurchFathers(
  q: string,
  limit?: number,
  locale?: string
): Promise<ChurchFatherSummary[]> {
  const params = buildParams({ q, limit: limit?.toString(), locale });
  return fetchJson(`${BASE}/fathers/search${params}`);
}

export function getFatherStatements(params?: {
  topic?: string;
  century?: number;
  tradition?: string;
  page?: number;
  limit?: number;
  locale?: string;
}): Promise<TextualStatementsListResponse> {
  const q = buildParams({
    topic: params?.topic,
    century: params?.century?.toString(),
    tradition: params?.tradition,
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    locale: params?.locale,
  });
  return fetchJson(`${BASE}/fathers/statements${q}`);
}

export function getFatherStatementsById(
  id: number,
  locale?: string
): Promise<TextualStatementDTO[]> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/fathers/${id}/statements${q}`);
}

export function searchStatements(
  q: string,
  limit?: number,
  locale?: string
): Promise<TextualStatementDTO[]> {
  const params = buildParams({ q, limit: limit?.toString(), locale });
  return fetchJson(`${BASE}/fathers/statements/search${params}`);
}

export function getTopicsSummary(): Promise<TopicsSummaryResponse> {
  return fetchJson(`${BASE}/fathers/statements/topics/summary`);
}

// ── Visitor Analytics API ──

import type {
  AnalyticsOverview,
  LiveVisitorDTO,
  FilterValuesResponse,
  SessionsPageResponse,
  SessionsPageCompleteResponse,
  VisitorSessionComplete,
  PageViewDTO as PageViewDTOType,
  TimelineAnalyticsResponse,
  HeatmapResponse,
  VisitorsListResponse,
  VisitorSummaryDTO,
  DistributionResponse,
  TopPageDTO,
  TopReferrerDTO,
  TrendsResponse,
  SessionFilters,
} from "@/types";

function visitorParams(filters: SessionFilters): string {
  const p: Record<string, string | undefined> = {};
  if (filters.from) p.from = filters.from;
  if (filters.to) p.to = filters.to;
  if (filters.days !== undefined) p.days = filters.days.toString();
  if (filters.browser) p.browser = filters.browser;
  if (filters.os) p.os = filters.os;
  if (filters.deviceType) p.deviceType = filters.deviceType;
  if (filters.language) p.language = filters.language;
  if (filters.timezone) p.timezone = filters.timezone;
  if (filters.ip) p.ip = filters.ip;
  if (filters.visitorId) p.visitorId = filters.visitorId;
  if (filters.fingerprint) p.fingerprint = filters.fingerprint;
  if (filters.referrer) p.referrer = filters.referrer;
  if (filters.returning !== undefined) p.returning = filters.returning.toString();
  if (filters.minLoadTime !== undefined) p.minLoadTime = filters.minLoadTime.toString();
  if (filters.maxLoadTime !== undefined) p.maxLoadTime = filters.maxLoadTime.toString();
  if (filters.sort) p.sort = filters.sort;
  if (filters.order) p.order = filters.order;
  if (filters.page !== undefined) p.page = filters.page.toString();
  if (filters.limit !== undefined) p.limit = filters.limit.toString();
  if (filters.view) p.view = filters.view;
  return buildParams(p);
}

export function getAnalyticsOverview(filters: SessionFilters = {}): Promise<AnalyticsOverview> {
  return fetchJson(`${BASE}/visitor/analytics/overview${visitorParams(filters)}`);
}

export function getAnalyticsLive(): Promise<LiveVisitorDTO[]> {
  return fetchJson(`${BASE}/visitor/analytics/live`);
}

export function getAnalyticsFilterValues(): Promise<FilterValuesResponse> {
  return fetchJson(`${BASE}/visitor/analytics/filters/values`);
}

export function getAnalyticsSessions(filters: SessionFilters = {}): Promise<SessionsPageResponse | SessionsPageCompleteResponse> {
  return fetchJson(`${BASE}/visitor/analytics/sessions${visitorParams(filters)}`);
}

export function getAnalyticsSessionDetail(sessionId: string): Promise<VisitorSessionComplete> {
  return fetchJson(`${BASE}/visitor/analytics/sessions/${sessionId}`);
}

export function getAnalyticsSessionPageviews(sessionId: string): Promise<PageViewDTOType[]> {
  return fetchJson(`${BASE}/visitor/analytics/sessions/${sessionId}/pageviews`);
}

export function getAnalyticsTimelineSessions(filters: SessionFilters & { granularity?: string; breakdown?: string } = {}): Promise<TimelineAnalyticsResponse> {
  const p: Record<string, string | undefined> = {};
  if (filters.from) p.from = filters.from;
  if (filters.to) p.to = filters.to;
  if (filters.days !== undefined) p.days = filters.days.toString();
  if (filters.granularity) p.granularity = filters.granularity;
  if (filters.breakdown) p.breakdown = filters.breakdown;
  return fetchJson(`${BASE}/visitor/analytics/timeline/sessions${buildParams(p)}`);
}

export function getAnalyticsTimelinePageviews(filters: SessionFilters & { granularity?: string; breakdown?: string } = {}): Promise<TimelineAnalyticsResponse> {
  const p: Record<string, string | undefined> = {};
  if (filters.from) p.from = filters.from;
  if (filters.to) p.to = filters.to;
  if (filters.days !== undefined) p.days = filters.days.toString();
  if (filters.granularity) p.granularity = filters.granularity;
  if (filters.breakdown) p.breakdown = filters.breakdown;
  return fetchJson(`${BASE}/visitor/analytics/timeline/pageviews${buildParams(p)}`);
}

export function getAnalyticsHeatmap(filters: SessionFilters = {}): Promise<HeatmapResponse> {
  return fetchJson(`${BASE}/visitor/analytics/timeline/heatmap${visitorParams(filters)}`);
}

export function getAnalyticsVisitors(filters: SessionFilters & { returning?: boolean } = {}): Promise<VisitorsListResponse> {
  return fetchJson(`${BASE}/visitor/analytics/visitors${visitorParams(filters)}`);
}

export function getAnalyticsVisitorProfile(visitorId: string): Promise<VisitorSummaryDTO> {
  return fetchJson(`${BASE}/visitor/analytics/visitors/${visitorId}`);
}

export function getAnalyticsVisitorSessions(visitorId: string, page = 1, limit = 50): Promise<SessionsPageResponse> {
  return fetchJson(`${BASE}/visitor/analytics/visitors/${visitorId}/sessions${buildParams({ page: page.toString(), limit: limit.toString() })}`);
}

export function getAnalyticsTopPages(filters: SessionFilters & { limit?: number } = {}): Promise<TopPageDTO[]> {
  return fetchJson(`${BASE}/visitor/analytics/top/pages${visitorParams(filters)}`);
}

export function getAnalyticsTopReferrers(filters: SessionFilters & { limit?: number } = {}): Promise<TopReferrerDTO[]> {
  return fetchJson(`${BASE}/visitor/analytics/top/referrers${visitorParams(filters)}`);
}

export function getAnalyticsDistribution(field: string, filters: SessionFilters = {}): Promise<DistributionResponse> {
  const base = visitorParams(filters);
  const sep = base ? "&" : "?";
  return fetchJson(`${BASE}/visitor/analytics/distribution${base}${sep}field=${field}`);
}

export function getAnalyticsTrends(days = 30): Promise<TrendsResponse> {
  return fetchJson(`${BASE}/visitor/analytics/trends?days=${days}`);
}
