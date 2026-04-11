import type {
  NtMetricsResponse,
  BookMetricsResponse,
  ManuscriptsCountResponse,
  StatsOverviewResponse,
} from "@/types";
import { BASE_URL, fetchJson } from "./client";

const BASE = BASE_URL;

export function getNtMetrics(): Promise<NtMetricsResponse> {
  return fetchJson(`${BASE}/metrics/nt`);
}

export function getBookMetrics(book: string): Promise<BookMetricsResponse> {
  return fetchJson(`${BASE}/metrics/${encodeURIComponent(book)}`);
}

export function getManuscriptsCount(): Promise<ManuscriptsCountResponse> {
  return fetchJson(`${BASE}/stats/manuscripts-count`);
}

export function getStatsOverview(): Promise<StatsOverviewResponse> {
  return fetchJson(`${BASE}/stats/overview`);
}
