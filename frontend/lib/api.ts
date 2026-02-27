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
