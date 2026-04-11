import type {
  ChurchFathersListResponse,
  ChurchFatherDetail,
  ChurchFatherSummary,
  TextualStatementsListResponse,
  TextualStatementDTO,
  TopicsSummaryResponse,
  PatristicStatsDTO,
  CouncilSummaryDTO,
} from "@/types";
import { BASE_URL, fetchJson, buildParams } from "./client";

const BASE = BASE_URL;

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

export function getPatristicStats(): Promise<PatristicStatsDTO> {
  return fetchJson(`${BASE}/fathers/stats`);
}
