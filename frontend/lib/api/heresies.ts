import type {
  HeresiesListResponse,
  HeresyDetailDTO,
  CouncilSummaryDTO,
} from "@/types";
import { BASE_URL, fetchJson, buildParams } from "./client";

const BASE = BASE_URL;

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
