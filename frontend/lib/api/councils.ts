import type {
  CouncilsListResponse,
  CouncilDetailDTO,
  CouncilSummaryDTO,
  CouncilFatherDTO,
  CouncilCanonDTO,
  CouncilMapPointDTO,
  CouncilTypeSummaryDTO,
  HeresySummaryDTO,
  SourceClaimDTO,
} from "@/types";
import { BASE_URL, fetchJson, buildParams } from "./client";

const BASE = BASE_URL;

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
