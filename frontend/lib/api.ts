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
  CouncilsListResponse,
  CouncilDetailDTO,
  CouncilSummaryDTO,
  CouncilFatherDTO,
  CouncilCanonDTO,
  HeresiesListResponse,
  HeresyDetailDTO,
  HeresySummaryDTO,
  CouncilMapPointDTO,
  CouncilTypeSummaryDTO,
  SourceDTO,
  SourceClaimDTO,
  PhaseStatusDTO,
  CacheStatsDTO,
} from "@/types";

import type { UserDTO, LoginResponse } from "@/types";

const BASE = "/api";

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

// ── Token storage (localStorage primary + cookie fallback) ──

const TOKEN_KEY = "observatory_token";

function getCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

function setCookie(name: string, value: string, maxAge: number) {
  if (typeof document === "undefined") return;
  const secure = window.location.protocol === "https:" ? "; Secure" : "";
  document.cookie = `${name}=${encodeURIComponent(value)}; max-age=${maxAge}; path=/; SameSite=Strict${secure}`;
}

function deleteCookie(name: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; max-age=0; path=/; SameSite=Strict`;
}

export function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY) ?? getCookie(TOKEN_KEY);
}

export function setAuthToken(token: string, maxAge: number) {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, token);
  setCookie(TOKEN_KEY, token, maxAge);
}

export function clearAuthToken() {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
  deleteCookie(TOKEN_KEY);
}

// ── Auth error class ──

export class AuthError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = "AuthError";
  }
}

// ── Authenticated fetch ──

async function fetchJsonAuth<T>(url: string, init?: RequestInit): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = { ...(init?.headers as Record<string, string>) };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(url, { ...init, headers });

  if (res.status === 401) {
    clearAuthToken();
    throw new AuthError(401, "Authentication required");
  }
  if (res.status === 403) {
    let msg = "Access denied";
    try {
      const body = await res.json();
      if (body?.message) msg = body.message;
    } catch { /* use default */ }
    throw new AuthError(403, msg);
  }
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

// ── Auth / User Management API ──

export async function loginWithGoogle(credential: string): Promise<LoginResponse> {
  const res = await fetch(`${BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ credential }),
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`API error ${res.status}: ${body}`);
  }
  return res.json();
}

export function getAuthMe(): Promise<UserDTO> {
  return fetchJsonAuth(`${BASE}/auth/me`);
}

export function getUsers(): Promise<UserDTO[]> {
  return fetchJsonAuth(`${BASE}/auth/users`);
}

export function createUser(email: string, displayName: string, role: string): Promise<UserDTO> {
  return fetchJsonAuth(`${BASE}/auth/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, displayName, role }),
  });
}

export function updateUserRole(id: number, role: string): Promise<{ ok: boolean }> {
  return fetchJsonAuth(`${BASE}/auth/users/${id}/role`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ role }),
  });
}

export function deleteUser(id: number): Promise<{ ok: boolean }> {
  return fetchJsonAuth(`${BASE}/auth/users/${id}`, { method: "DELETE" });
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
  yearMin?: number;
  yearMax?: number;
}): Promise<ManuscriptSummary[]> {
  const q = buildParams({
    type: params?.type,
    century: params?.century?.toString(),
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    yearMin: params?.yearMin?.toString(),
    yearMax: params?.yearMax?.toString(),
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
  return fetchJsonAuth(`${BASE}/admin/ingestion/status`);
}

export function triggerIngestion(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/ingestion/run`, { method: "POST" });
}

export function resetAndReIngest(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/ingestion/reset`, { method: "POST" });
}

export function triggerPatristicSeed(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/patristic/seed`, { method: "POST" });
}

export function triggerPatristicTranslate(force: boolean = false): Promise<{ message: string }> {
  const params = force ? "?force=true" : "";
  return fetchJsonAuth(`${BASE}/admin/patristic/translate${params}`, { method: "POST" });
}

export function triggerDatingEnrichment(
  domain: "fathers" | "manuscripts" | "all" = "fathers",
  limit: number = 50,
): Promise<{ message: string }> {
  const params = buildParams({ domain, limit: limit.toString() });
  return fetchJsonAuth(`${BASE}/admin/enrich-dating${params}`, { method: "POST" });
}

export function getChurchFathers(params?: {
  century?: number;
  tradition?: string;
  page?: number;
  limit?: number;
  locale?: string;
  yearMin?: number;
  yearMax?: number;
  yearMinFrom?: number;
  yearMinTo?: number;
}): Promise<ChurchFathersListResponse> {
  const q = buildParams({
    century: params?.century?.toString(),
    tradition: params?.tradition,
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    locale: params?.locale,
    yearMin: params?.yearMin?.toString(),
    yearMax: params?.yearMax?.toString(),
    yearMinFrom: params?.yearMinFrom?.toString(),
    yearMinTo: params?.yearMinTo?.toString(),
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

export function getFatherCouncils(id: number, locale?: string): Promise<CouncilSummaryDTO[]> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/fathers/${id}/councils${q}`);
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
  yearMin?: number;
  yearMax?: number;
}): Promise<TextualStatementsListResponse> {
  const q = buildParams({
    topic: params?.topic,
    century: params?.century?.toString(),
    tradition: params?.tradition,
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    locale: params?.locale,
    yearMin: params?.yearMin?.toString(),
    yearMax: params?.yearMax?.toString(),
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

// ── Councils / Heresies API ──

export function getCouncils(params?: {
  century?: number;
  type?: string;
  yearMin?: number;
  yearMax?: number;
  page?: number;
  limit?: number;
  locale?: string;
}): Promise<CouncilsListResponse> {
  const q = buildParams({
    century: params?.century?.toString(),
    type: params?.type,
    yearMin: params?.yearMin?.toString(),
    yearMax: params?.yearMax?.toString(),
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    locale: params?.locale,
  });
  return fetchJson(`${BASE}/councils${q}`);
}

export function searchCouncils(q: string, limit = 20, locale?: string): Promise<CouncilSummaryDTO[]> {
  const params = buildParams({ q, limit: limit.toString(), locale });
  return fetchJson(`${BASE}/councils/search${params}`);
}

export function getCouncilTypeSummary(): Promise<CouncilTypeSummaryDTO[]> {
  return fetchJson(`${BASE}/councils/types/summary`);
}

export function getCouncilMapPoints(): Promise<CouncilMapPointDTO[]> {
  return fetchJson(`${BASE}/councils/map`);
}

export function getCouncilDetail(slug: string, locale?: string): Promise<CouncilDetailDTO> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/councils/${encodeURIComponent(slug)}${q}`);
}

export function getCouncilFathers(slug: string, locale?: string): Promise<CouncilFatherDTO[]> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/councils/${encodeURIComponent(slug)}/fathers${q}`);
}

export function getCouncilCanons(slug: string, page = 1, limit = 50): Promise<CouncilCanonDTO[]> {
  const q = buildParams({ page: page.toString(), limit: limit.toString() });
  return fetchJson(`${BASE}/councils/${encodeURIComponent(slug)}/canons${q}`);
}

export function getCouncilHeresies(slug: string, locale?: string): Promise<HeresySummaryDTO[]> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/councils/${encodeURIComponent(slug)}/heresies${q}`);
}

export function getCouncilSources(slug: string): Promise<SourceClaimDTO[]> {
  return fetchJson(`${BASE}/councils/${encodeURIComponent(slug)}/sources`);
}

export function getHeresies(params?: { page?: number; limit?: number; locale?: string }): Promise<HeresiesListResponse> {
  const q = buildParams({
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    locale: params?.locale,
  });
  return fetchJson(`${BASE}/heresies${q}`);
}

export function getHeresyDetail(slug: string, locale?: string): Promise<HeresyDetailDTO> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/heresies/${encodeURIComponent(slug)}${q}`);
}

export function getHeresyCouncils(slug: string, locale?: string): Promise<CouncilSummaryDTO[]> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/heresies/${encodeURIComponent(slug)}/councils${q}`);
}

export function getSources(): Promise<SourceDTO[]> {
  return fetchJson(`${BASE}/sources`);
}

// ── Council ingestion admin API ──

export function getCouncilIngestionPhases(): Promise<PhaseStatusDTO[]> {
  return fetchJsonAuth(`${BASE}/admin/councils/ingestion/phases`);
}

export function runCouncilPhase(phase: string): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/councils/ingestion/run/${encodeURIComponent(phase)}`, { method: "POST" });
}

export function runCouncilPhases(phases: string[]): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/councils/ingestion/run`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phases }),
  });
}

export function runAllCouncilPhases(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/councils/ingestion/run-all`, { method: "POST" });
}

export function getCouncilIngestionCache(): Promise<CacheStatsDTO> {
  return fetchJsonAuth(`${BASE}/admin/councils/ingestion/cache`);
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
