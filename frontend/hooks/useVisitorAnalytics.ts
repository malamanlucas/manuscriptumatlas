import { useQuery } from "@tanstack/react-query";
import {
  getAnalyticsOverview,
  getAnalyticsLive,
  getAnalyticsFilterValues,
  getAnalyticsSessions,
  getAnalyticsSessionDetail,
  getAnalyticsSessionPageviews,
  getAnalyticsTimelineSessions,
  getAnalyticsTimelinePageviews,
  getAnalyticsHeatmap,
  getAnalyticsVisitors,
  getAnalyticsVisitorProfile,
  getAnalyticsVisitorSessions,
  getAnalyticsTopPages,
  getAnalyticsTopReferrers,
  getAnalyticsDistribution,
  getAnalyticsTrends,
} from "@/lib/api";
import type { SessionFilters } from "@/types";

export function useAnalyticsOverview(filters: SessionFilters = {}) {
  return useQuery({
    queryKey: ["analytics", "overview", filters],
    queryFn: () => getAnalyticsOverview(filters),
    refetchInterval: 10000,
  });
}

export function useAnalyticsLive() {
  return useQuery({
    queryKey: ["analytics", "live"],
    queryFn: () => getAnalyticsLive(),
    refetchInterval: 10000,
  });
}

export function useAnalyticsFilterValues() {
  return useQuery({
    queryKey: ["analytics", "filterValues"],
    queryFn: () => getAnalyticsFilterValues(),
    staleTime: 60000,
  });
}

export function useAnalyticsSessions(filters: SessionFilters = {}) {
  return useQuery({
    queryKey: ["analytics", "sessions", filters],
    queryFn: () => getAnalyticsSessions(filters),
  });
}

export function useAnalyticsSessionDetail(sessionId: string | null) {
  return useQuery({
    queryKey: ["analytics", "session", sessionId],
    queryFn: () => getAnalyticsSessionDetail(sessionId!),
    enabled: !!sessionId,
  });
}

export function useAnalyticsSessionPageviews(sessionId: string | null) {
  return useQuery({
    queryKey: ["analytics", "session", sessionId, "pageviews"],
    queryFn: () => getAnalyticsSessionPageviews(sessionId!),
    enabled: !!sessionId,
  });
}

export function useAnalyticsTimelineSessions(
  filters: SessionFilters & { granularity?: string; breakdown?: string } = {}
) {
  return useQuery({
    queryKey: ["analytics", "timeline", "sessions", filters],
    queryFn: () => getAnalyticsTimelineSessions(filters),
    refetchInterval: 30000,
  });
}

export function useAnalyticsTimelinePageviews(
  filters: SessionFilters & { granularity?: string; breakdown?: string } = {}
) {
  return useQuery({
    queryKey: ["analytics", "timeline", "pageviews", filters],
    queryFn: () => getAnalyticsTimelinePageviews(filters),
    refetchInterval: 30000,
  });
}

export function useAnalyticsHeatmap(filters: SessionFilters = {}) {
  return useQuery({
    queryKey: ["analytics", "heatmap", filters],
    queryFn: () => getAnalyticsHeatmap(filters),
    staleTime: 60000,
  });
}

export function useAnalyticsVisitors(
  filters: SessionFilters & { returning?: boolean } = {}
) {
  return useQuery({
    queryKey: ["analytics", "visitors", filters],
    queryFn: () => getAnalyticsVisitors(filters),
  });
}

export function useAnalyticsVisitorProfile(visitorId: string | null) {
  return useQuery({
    queryKey: ["analytics", "visitor", visitorId],
    queryFn: () => getAnalyticsVisitorProfile(visitorId!),
    enabled: !!visitorId,
  });
}

export function useAnalyticsVisitorSessions(
  visitorId: string | null,
  page = 1,
  limit = 50
) {
  return useQuery({
    queryKey: ["analytics", "visitor", visitorId, "sessions", page, limit],
    queryFn: () => getAnalyticsVisitorSessions(visitorId!, page, limit),
    enabled: !!visitorId,
  });
}

export function useAnalyticsTopPages(filters: SessionFilters = {}) {
  return useQuery({
    queryKey: ["analytics", "topPages", filters],
    queryFn: () => getAnalyticsTopPages(filters),
    refetchInterval: 10000,
  });
}

export function useAnalyticsTopReferrers(filters: SessionFilters = {}) {
  return useQuery({
    queryKey: ["analytics", "topReferrers", filters],
    queryFn: () => getAnalyticsTopReferrers(filters),
    refetchInterval: 10000,
  });
}

export function useAnalyticsDistribution(
  field: string,
  filters: SessionFilters = {}
) {
  return useQuery({
    queryKey: ["analytics", "distribution", field, filters],
    queryFn: () => getAnalyticsDistribution(field, filters),
    refetchInterval: 10000,
  });
}

export function useAnalyticsTrends(days = 30) {
  return useQuery({
    queryKey: ["analytics", "trends", days],
    queryFn: () => getAnalyticsTrends(days),
    staleTime: 60000,
  });
}
