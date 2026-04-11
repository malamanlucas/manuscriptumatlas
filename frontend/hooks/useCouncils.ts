"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import {
  getCouncils,
  searchCouncils,
  getCouncilDetail,
  getCouncilTypeSummary,
  getCouncilMapPoints,
  getCouncilCanons,
  getCouncilFathers,
  getCouncilHeresies,
  getCouncilSources,
} from "@/lib/api";

export function useCouncils(params?: {
  century?: number;
  type?: string;
  yearMin?: number;
  yearMax?: number;
  page?: number;
  limit?: number;
}) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["councils", "list", locale, params?.century, params?.type, params?.yearMin, params?.yearMax, params?.page, params?.limit],
    queryFn: () => getCouncils({ ...params, locale }),
    staleTime: 15_000,
  });
}

export function useSearchCouncils(query: string, limit = 20) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["councils", "search", locale, query, limit],
    queryFn: () => searchCouncils(query, limit, locale),
    enabled: query.trim().length > 1,
    staleTime: 15_000,
  });
}

export function useCouncilDetail(slug: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["councils", "detail", slug, locale],
    queryFn: () => getCouncilDetail(slug, locale),
    enabled: !!slug,
    staleTime: 15_000,
  });
}

export function useCouncilTypeSummary() {
  return useQuery({
    queryKey: ["councils", "types", "summary"],
    queryFn: () => getCouncilTypeSummary(),
    staleTime: 60_000,
  });
}

export function useCouncilMapPoints() {
  return useQuery({
    queryKey: ["councils", "map"],
    queryFn: () => getCouncilMapPoints(),
    staleTime: 60_000,
  });
}

export function useCouncilCanons(slug: string, page = 1, limit = 50) {
  return useQuery({
    queryKey: ["councils", slug, "canons", page, limit],
    queryFn: () => getCouncilCanons(slug, page, limit),
    enabled: !!slug,
  });
}

export function useCouncilFathers(slug: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["councils", slug, "fathers", locale],
    queryFn: () => getCouncilFathers(slug, locale),
    enabled: !!slug,
  });
}

export function useCouncilHeresies(slug: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["councils", slug, "heresies", locale],
    queryFn: () => getCouncilHeresies(slug, locale),
    enabled: !!slug,
  });
}

export function useCouncilSources(slug: string) {
  return useQuery({
    queryKey: ["councils", slug, "sources"],
    queryFn: () => getCouncilSources(slug),
    enabled: !!slug,
  });
}
