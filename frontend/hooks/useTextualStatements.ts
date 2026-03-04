"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import {
  getFatherStatements,
  getFatherStatementsById,
  searchStatements,
  getTopicsSummary,
} from "@/lib/api";

export function useTextualStatements(params?: {
  topic?: string;
  century?: number;
  tradition?: string;
  page?: number;
  limit?: number;
  yearMin?: number;
  yearMax?: number;
}) {
  const locale = useLocale();
  return useQuery({
    queryKey: [
      "statements",
      locale,
      params?.topic,
      params?.century,
      params?.tradition,
      params?.page,
      params?.limit,
      params?.yearMin,
      params?.yearMax,
    ],
    queryFn: () => getFatherStatements({ ...params, locale }),
    staleTime: 5 * 60 * 1000,
  });
}

export function useFatherStatements(fatherId: number | null) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["statements", "father", locale, fatherId],
    queryFn: () => getFatherStatementsById(fatherId!, locale),
    enabled: fatherId !== null,
    staleTime: 5 * 60 * 1000,
  });
}

export function useSearchStatements(query: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["statements", "search", locale, query],
    queryFn: () => searchStatements(query, undefined, locale),
    enabled: query.length >= 2,
    staleTime: 5 * 60 * 1000,
  });
}

export function useTopicsSummary() {
  return useQuery({
    queryKey: ["statements", "topics", "summary"],
    queryFn: () => getTopicsSummary(),
    staleTime: 5 * 60 * 1000,
  });
}
