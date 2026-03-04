"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import {
  getChurchFathers,
  getChurchFatherDetail,
  searchChurchFathers,
} from "@/lib/api";

export function useChurchFathers(params?: {
  century?: number;
  tradition?: string;
  page?: number;
  limit?: number;
  yearMin?: number;
  yearMax?: number;
}) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["fathers", locale, params?.century, params?.tradition, params?.page, params?.limit, params?.yearMin, params?.yearMax],
    queryFn: () => getChurchFathers({ ...params, locale }),
    staleTime: 5 * 60 * 1000,
  });
}

export function useChurchFatherDetail(id: number | null) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["fathers", "detail", locale, id],
    queryFn: () => getChurchFatherDetail(id!, locale),
    enabled: id !== null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSearchChurchFathers(query: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["fathers", "search", locale, query],
    queryFn: () => searchChurchFathers(query, undefined, locale),
    enabled: query.length >= 2,
    staleTime: 5 * 60 * 1000,
  });
}
