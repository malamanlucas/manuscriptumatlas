import type {
  LlmUsageDashboardDTO,
  LlmUsageLogDTO,
  QueueStatsDTO,
  RateLimiterStatusDTO,
} from "@/types";
import { BASE_URL, fetchJsonAuth } from "./client";

const BASE = BASE_URL;

export function fetchLlmUsageDashboard(period: string = "7d"): Promise<LlmUsageDashboardDTO> {
  return fetchJsonAuth(`${BASE}/admin/llm/usage?period=${period}`);
}

export function fetchLlmUsageLogs(limit: number = 50, provider?: string): Promise<LlmUsageLogDTO[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  if (provider) params.set("provider", provider);
  return fetchJsonAuth(`${BASE}/admin/llm/usage/logs?${params}`);
}

export function fetchLlmRateLimits(): Promise<RateLimiterStatusDTO[]> {
  return fetchJsonAuth(`${BASE}/admin/llm/rate-limits`);
}

export function fetchLlmErrorsByType(
  errorType: string,
  limit: number = 20,
  period: string = "7d"
): Promise<LlmUsageLogDTO[]> {
  const params = new URLSearchParams({ type: errorType, limit: String(limit), period });
  return fetchJsonAuth(`${BASE}/admin/llm/usage/errors?${params}`);
}

// ── LLM Queue ──

export function fetchLlmQueueStats(): Promise<QueueStatsDTO> {
  return fetchJsonAuth(`${BASE}/admin/llm/queue/stats`);
}

export function retryFailedQueueItems(phase?: string): Promise<{ message: string }> {
  const params = phase ? `?phase=${encodeURIComponent(phase)}` : "";
  return fetchJsonAuth(`${BASE}/admin/llm/queue/retry${params}`, { method: "POST" });
}

export function unstickProcessingItems(phase?: string): Promise<{ message: string }> {
  const params = phase ? `?phase=${encodeURIComponent(phase)}` : "";
  return fetchJsonAuth(`${BASE}/admin/llm/queue/unstick${params}`, { method: "POST" });
}

export function clearQueuePhase(phase: string): Promise<{ message: string }> {
  return fetchJsonAuth(`${BASE}/admin/llm/queue/${encodeURIComponent(phase)}`, { method: "DELETE" });
}
