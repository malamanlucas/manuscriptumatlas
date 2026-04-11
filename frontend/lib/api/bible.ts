import type {
  BibleVersionDTO,
  BibleBookDTO,
  BibleChapterResponse,
  BibleVerseTextDTO,
  ResolvedReferenceDTO,
  PhaseStatusDTO,
  InterlinearChapterDTO,
  InterlinearVerseDTO,
  StrongsConcordanceDTO,
  LexiconEntryDTO,
  BibleCompareResponse,
  BibleSearchResponse,
} from "@/types";
import { BASE_URL, fetchJson, fetchJsonAuth } from "./client";

const BASE = BASE_URL;

export function getBibleVersions(testament?: string): Promise<BibleVersionDTO[]> {
  const params = testament ? `?testament=${testament}` : "";
  return fetchJson(`${BASE}/bible/versions${params}`);
}

export function getBibleBooks(testament?: string): Promise<BibleBookDTO[]> {
  const params = testament ? `?testament=${testament}` : "";
  return fetchJson(`${BASE}/bible/books${params}`);
}

export function getBibleChapter(version: string, book: string, chapter: number): Promise<BibleChapterResponse> {
  return fetchJson(`${BASE}/bible/read/${encodeURIComponent(version)}/${encodeURIComponent(book)}/${chapter}`);
}

export function getBibleVerse(version: string, book: string, chapter: number, verse: number): Promise<BibleVerseTextDTO> {
  return fetchJson(`${BASE}/bible/read/${encodeURIComponent(version)}/${encodeURIComponent(book)}/${chapter}/${verse}`);
}

export function resolveRef(reference: string, locale: string = "en"): Promise<ResolvedReferenceDTO> {
  return fetchJson(`${BASE}/bible/ref/${encodeURIComponent(reference)}?locale=${locale}`);
}

export function getBibleIngestionPhases(): Promise<PhaseStatusDTO[]> {
  return fetchJsonAuth(`${BASE}/admin/bible/ingestion/phases`);
}

export function runBiblePhase(phase: string): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/bible/ingestion/run/${encodeURIComponent(phase)}`, { method: "POST" });
}

export function runBiblePhases(phases: string[]): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/bible/ingestion/run`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phases }),
  });
}

export function runAllBiblePhases(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/bible/ingestion/run-all`, { method: "POST" });
}

export function clearBibleGlosses(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/bible/glosses/clear`, { method: "POST" });
}

export function getBibleInterlinearChapter(book: string, chapter: number, alignVersion?: string): Promise<InterlinearChapterDTO> {
  const params = alignVersion ? `?alignVersion=${encodeURIComponent(alignVersion)}` : "";
  return fetchJson(`${BASE}/bible/interlinear/${encodeURIComponent(book)}/${chapter}${params}`);
}

export function getBibleInterlinearVerse(book: string, chapter: number, verse: number, alignVersion?: string): Promise<InterlinearVerseDTO> {
  const params = alignVersion ? `?alignVersion=${encodeURIComponent(alignVersion)}` : "";
  return fetchJson(`${BASE}/bible/interlinear/${encodeURIComponent(book)}/${chapter}/${verse}${params}`);
}

export function getStrongsConcordance(strongsNumber: string, page: number = 1, limit: number = 50): Promise<StrongsConcordanceDTO> {
  return fetchJson(`${BASE}/bible/strongs/${encodeURIComponent(strongsNumber)}?page=${page}&limit=${limit}`);
}

export function getLexiconEntry(strongsNumber: string, locale: string = "en"): Promise<LexiconEntryDTO> {
  return fetchJson(`${BASE}/bible/lexicon/${encodeURIComponent(strongsNumber)}?locale=${locale}`);
}

export function compareBibleChapter(book: string, chapter: number, versions: string[]): Promise<BibleCompareResponse> {
  return fetchJson(`${BASE}/bible/compare/${encodeURIComponent(book)}/${chapter}?versions=${versions.join(",")}`);
}

export function compareBibleVerse(book: string, chapter: number, verse: number, versions: string[]): Promise<BibleCompareResponse> {
  return fetchJson(`${BASE}/bible/compare/${encodeURIComponent(book)}/${chapter}/${verse}?versions=${versions.join(",")}`);
}

export function searchBible(query: string, options?: { version?: string; testament?: string; book?: string; locale?: string; page?: number; limit?: number }): Promise<BibleSearchResponse> {
  const params = new URLSearchParams({ q: query });
  if (options?.version) params.set("version", options.version);
  if (options?.testament) params.set("testament", options.testament);
  if (options?.book) params.set("book", options.book);
  if (options?.locale) params.set("locale", options.locale);
  if (options?.page) params.set("page", String(options.page));
  if (options?.limit) params.set("limit", String(options.limit));
  return fetchJson(`${BASE}/bible/search?${params}`);
}
