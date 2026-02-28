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
