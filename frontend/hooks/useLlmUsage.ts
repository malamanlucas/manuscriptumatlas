"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchLlmUsageDashboard, fetchLlmRateLimits, fetchLlmErrorsByType } from "@/lib/api";

export function useLlmDashboard(period: string) {
  return useQuery({
    queryKey: ["llm-usage", "dashboard", period],
    queryFn: () => fetchLlmUsageDashboard(period),
    refetchInterval: 15_000,
    staleTime: 5_000,
  });
}

export function useLlmRateLimits() {
  return useQuery({
    queryKey: ["llm-usage", "rate-limits"],
    queryFn: fetchLlmRateLimits,
    refetchInterval: 15_000,
    staleTime: 5_000,
  });
}

export function useLlmErrorsByType(errorType: string | null, period: string) {
  return useQuery({
    queryKey: ["llm-usage", "errors", errorType, period],
    queryFn: () => fetchLlmErrorsByType(errorType!, 20, period),
    enabled: !!errorType,
    staleTime: 10_000,
  });
}
