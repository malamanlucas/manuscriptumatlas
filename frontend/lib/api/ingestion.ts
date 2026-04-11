import type {
  IngestionStatusResponse,
  PhaseStatusDTO,
  CacheStatsDTO,
} from "@/types";
import { BASE_URL, fetchJsonAuth, buildParams } from "./client";

const BASE = BASE_URL;

// ── Global ingestion ──

export function getIngestionStatus(): Promise<IngestionStatusResponse> {
  return fetchJsonAuth(`${BASE}/admin/ingestion/status`);
}

export function triggerIngestion(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/ingestion/run`, { method: "POST" });
}

export function resetAndReIngest(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/ingestion/reset`, { method: "POST" });
}

// ── Patristic ingestion (legacy endpoints) ──

export function triggerPatristicSeed(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/patristic/seed`, { method: "POST" });
}

export function triggerPatristicTranslate(force: boolean = false): Promise<{ message: string }> {
  const params = force ? "?force=true" : "";
  return fetchJsonAuth(`${BASE}/admin/patristic/translate${params}`, { method: "POST" });
}

// ── Dating enrichment ──

export function triggerDatingEnrichment(
  domain: "fathers" | "manuscripts" | "all" = "fathers",
  limit: number = 50,
): Promise<{ message: string }> {
  const params = buildParams({ domain, limit: limit.toString() });
  return fetchJsonAuth(`${BASE}/admin/enrich-dating${params}`, { method: "POST" });
}

// ── Domain reset ──

export function resetDomain(domain: "manuscripts" | "patristic" | "councils" | "bible" | "bible-layer1" | "bible-layer2" | "bible-layer3" | "bible-layer4"): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/reset/${domain}`, { method: "POST" });
}

// ── Manuscript ingestion phases ──

export function getManuscriptIngestionPhases(): Promise<PhaseStatusDTO[]> {
  return fetchJsonAuth(`${BASE}/admin/manuscripts/ingestion/phases`);
}

export function runManuscriptPhase(phase: string): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/manuscripts/ingestion/run/${encodeURIComponent(phase)}`, { method: "POST" });
}

export function runAllManuscriptPhases(): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/manuscripts/ingestion/run-all`, { method: "POST" });
}

// ── Patristic ingestion phases ──

export function getPatristicIngestionPhases(): Promise<PhaseStatusDTO[]> {
  return fetchJsonAuth(`${BASE}/admin/patristic/ingestion/phases`);
}

export function runPatristicPhase(phase: string, filter?: string): Promise<{ message: string }> {
  const params = filter ? `?filter=${encodeURIComponent(filter)}` : "";
  return fetchJsonAuth(`${BASE}/admin/patristic/ingestion/run/${encodeURIComponent(phase)}${params}`, { method: "POST" });
}

export function runPatristicPhases(phases: string[], filter?: string): Promise<{ message: string }> {
  const params = filter ? `?filter=${encodeURIComponent(filter)}` : "";
  return fetchJsonAuth(`${BASE}/admin/patristic/ingestion/run${params}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phases }),
  });
}

export function runAllPatristicPhases(filter?: string): Promise<{ message: string }> {
  const params = filter ? `?filter=${encodeURIComponent(filter)}` : "";
  return fetchJsonAuth(`${BASE}/admin/patristic/ingestion/run-all${params}`, { method: "POST" });
}

// ── Council ingestion phases ──

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
