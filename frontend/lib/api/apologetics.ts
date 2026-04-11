import type {
  ApologeticTopicsListResponse,
  ApologeticTopicDetailDTO,
  ApologeticTopicSummaryDTO,
  ApologeticResponseDTO,
} from "@/types";
import { BASE_URL, fetchJson, fetchJsonAuth, buildParams } from "./client";

const BASE = BASE_URL;

// ── Public reads ──

export function getApologeticTopics(params?: {
  page?: number;
  limit?: number;
  locale?: string;
  status?: string;
}): Promise<ApologeticTopicsListResponse> {
  const q = buildParams({
    page: params?.page?.toString(),
    limit: params?.limit?.toString(),
    locale: params?.locale,
    status: params?.status,
  });
  return fetchJson(`${BASE}/apologetics${q}`);
}

export function searchApologeticTopics(
  q: string,
  limit = 20,
  locale?: string
): Promise<ApologeticTopicSummaryDTO[]> {
  const params = buildParams({ q, limit: limit.toString(), locale });
  return fetchJson(`${BASE}/apologetics/search${params}`);
}

export function getApologeticTopicDetail(
  slug: string,
  locale?: string
): Promise<ApologeticTopicDetailDTO> {
  const q = buildParams({ locale });
  return fetchJson(`${BASE}/apologetics/${encodeURIComponent(slug)}${q}`);
}

// ── Authenticated writes ──

export function createApologeticTopic(
  prompt: string
): Promise<ApologeticTopicDetailDTO> {
  return fetchJsonAuth(`${BASE}/apologetics`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prompt }),
  });
}

export function updateApologeticTopic(
  id: number,
  data: { title?: string; body?: string; status?: string; bodyReviewed?: boolean }
): Promise<{ ok: boolean }> {
  return fetchJsonAuth(`${BASE}/apologetics/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export function createApologeticResponse(
  topicId: number,
  body: string,
  useAi = false
): Promise<ApologeticResponseDTO> {
  return fetchJsonAuth(`${BASE}/apologetics/${topicId}/responses`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ body, useAi }),
  });
}

export function updateApologeticResponse(
  id: number,
  data: { body?: string; bodyReviewed?: boolean; useAi?: boolean }
): Promise<{ ok: boolean }> {
  return fetchJsonAuth(`${BASE}/apologetics/responses/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
}

export function deleteApologeticResponse(id: number): Promise<{ ok: boolean }> {
  return fetchJsonAuth(`${BASE}/apologetics/responses/${id}`, {
    method: "DELETE",
  });
}
