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
import { BASE_URL, fetchJsonAuth, buildParams } from "./client";

const BASE = BASE_URL;

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
  return fetchJsonAuth(`${BASE}/visitor/analytics/overview${visitorParams(filters)}`);
}

export function getAnalyticsLive(): Promise<LiveVisitorDTO[]> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/live`);
}

export function getAnalyticsFilterValues(): Promise<FilterValuesResponse> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/filters/values`);
}

export function getAnalyticsSessions(filters: SessionFilters = {}): Promise<SessionsPageResponse | SessionsPageCompleteResponse> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/sessions${visitorParams(filters)}`);
}

export function getAnalyticsSessionDetail(sessionId: string): Promise<VisitorSessionComplete> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/sessions/${sessionId}`);
}

export function getAnalyticsSessionPageviews(sessionId: string): Promise<PageViewDTOType[]> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/sessions/${sessionId}/pageviews`);
}

export function getAnalyticsTimelineSessions(filters: SessionFilters & { granularity?: string; breakdown?: string } = {}): Promise<TimelineAnalyticsResponse> {
  const p: Record<string, string | undefined> = {};
  if (filters.from) p.from = filters.from;
  if (filters.to) p.to = filters.to;
  if (filters.days !== undefined) p.days = filters.days.toString();
  if (filters.granularity) p.granularity = filters.granularity;
  if (filters.breakdown) p.breakdown = filters.breakdown;
  return fetchJsonAuth(`${BASE}/visitor/analytics/timeline/sessions${buildParams(p)}`);
}

export function getAnalyticsTimelinePageviews(filters: SessionFilters & { granularity?: string; breakdown?: string } = {}): Promise<TimelineAnalyticsResponse> {
  const p: Record<string, string | undefined> = {};
  if (filters.from) p.from = filters.from;
  if (filters.to) p.to = filters.to;
  if (filters.days !== undefined) p.days = filters.days.toString();
  if (filters.granularity) p.granularity = filters.granularity;
  if (filters.breakdown) p.breakdown = filters.breakdown;
  return fetchJsonAuth(`${BASE}/visitor/analytics/timeline/pageviews${buildParams(p)}`);
}

export function getAnalyticsHeatmap(filters: SessionFilters = {}): Promise<HeatmapResponse> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/timeline/heatmap${visitorParams(filters)}`);
}

export function getAnalyticsVisitors(filters: SessionFilters & { returning?: boolean } = {}): Promise<VisitorsListResponse> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/visitors${visitorParams(filters)}`);
}

export function getAnalyticsVisitorProfile(visitorId: string): Promise<VisitorSummaryDTO> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/visitors/${visitorId}`);
}

export function getAnalyticsVisitorSessions(visitorId: string, page = 1, limit = 50): Promise<SessionsPageResponse> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/visitors/${visitorId}/sessions${buildParams({ page: page.toString(), limit: limit.toString() })}`);
}

export function getAnalyticsTopPages(filters: SessionFilters & { limit?: number } = {}): Promise<TopPageDTO[]> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/top/pages${visitorParams(filters)}`);
}

export function getAnalyticsTopReferrers(filters: SessionFilters & { limit?: number } = {}): Promise<TopReferrerDTO[]> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/top/referrers${visitorParams(filters)}`);
}

export function getAnalyticsDistribution(field: string, filters: SessionFilters = {}): Promise<DistributionResponse> {
  const base = visitorParams(filters);
  const sep = base ? "&" : "?";
  return fetchJsonAuth(`${BASE}/visitor/analytics/distribution${base}${sep}field=${field}`);
}

export function getAnalyticsTrends(days = 30): Promise<TrendsResponse> {
  return fetchJsonAuth(`${BASE}/visitor/analytics/trends?days=${days}`);
}
