import type {
  CenturyCoverageResponse,
  FullCoverageResponse,
  BookChapterCoverageResponse,
  GospelCoverageResponse,
  MissingVersesResponse,
} from "@/types";
import { BASE_URL, fetchJson, buildParams } from "./client";

const BASE = BASE_URL;

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
