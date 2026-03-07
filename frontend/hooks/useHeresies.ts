"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { getHeresies, getHeresyDetail, getHeresyCouncils } from "@/lib/api";

export function useHeresies(params?: { page?: number; limit?: number }) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["heresies", "list", locale, params?.page, params?.limit],
    queryFn: () => getHeresies({ ...params, locale }),
    staleTime: 30_000,
  });
}

export function useHeresyDetail(slug: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["heresies", "detail", slug, locale],
    queryFn: () => getHeresyDetail(slug, locale),
    enabled: !!slug,
    staleTime: 30_000,
  });
}

export function useHeresyCouncils(slug: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["heresies", slug, "councils", locale],
    queryFn: () => getHeresyCouncils(slug, locale),
    enabled: !!slug,
    staleTime: 30_000,
  });
}
