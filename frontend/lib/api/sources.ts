import type { SourceDTO } from "@/types";
import { BASE_URL, fetchJson } from "./client";

const BASE = BASE_URL;

export function getSources(): Promise<SourceDTO[]> {
  return fetchJson(`${BASE}/sources`);
}
