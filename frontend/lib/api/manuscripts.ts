import type {
  ManuscriptSummary,
  ManuscriptDetailResponse,
  VerseManuscriptsResponse,
} from "@/types";
import { BASE_URL, fetchJson, buildParams } from "./client";

const BASE = BASE_URL;

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
