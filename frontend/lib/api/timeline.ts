import type { TimelineResponse } from "@/types";
import { BASE_URL, fetchJson, buildParams } from "./client";

const BASE = BASE_URL;

export function getTimeline(
  book?: string,
  type?: string
): Promise<TimelineResponse> {
  const params = buildParams({ book, type });
  return fetchJson(`${BASE}/timeline${params}`);
}

export function getTimelineFull(type?: string): Promise<TimelineResponse> {
  const params = buildParams({ type });
  return fetchJson(`${BASE}/timeline/full${params}`);
}
