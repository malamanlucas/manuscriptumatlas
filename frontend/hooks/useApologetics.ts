"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import {
  getApologeticTopics,
  searchApologeticTopics,
  getApologeticTopicDetail,
  createApologeticTopic,
  updateApologeticTopic,
  createApologeticResponse,
  updateApologeticResponse,
  deleteApologeticResponse,
} from "@/lib/api";

export function useApologeticTopics(params?: {
  page?: number;
  limit?: number;
  status?: string;
}) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["apologetics", "list", locale, params?.page, params?.limit, params?.status],
    queryFn: () => getApologeticTopics({ ...params, locale }),
    staleTime: 15_000,
  });
}

export function useSearchApologeticTopics(query: string, limit = 20) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["apologetics", "search", locale, query, limit],
    queryFn: () => searchApologeticTopics(query, limit, locale),
    enabled: query.trim().length > 1,
    staleTime: 15_000,
  });
}

export function useApologeticTopicDetail(slug: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["apologetics", "detail", slug, locale],
    queryFn: () => getApologeticTopicDetail(slug, locale),
    enabled: !!slug,
    staleTime: 15_000,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.status === "PROCESSING") return 5000;
      return false;
    },
  });
}

export function useCreateApologeticTopic() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (prompt: string) => createApologeticTopic(prompt),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["apologetics"] });
    },
  });
}

export function useUpdateApologeticTopic() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (args: { id: number; data: { title?: string; body?: string; status?: string; bodyReviewed?: boolean } }) =>
      updateApologeticTopic(args.id, args.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["apologetics"] });
    },
  });
}

export function useCreateApologeticResponse(topicId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (args: { body: string; useAi?: boolean }) =>
      createApologeticResponse(topicId, args.body, args.useAi),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["apologetics", "detail"] });
    },
  });
}

export function useUpdateApologeticResponse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (args: { id: number; data: { body?: string; bodyReviewed?: boolean; useAi?: boolean } }) =>
      updateApologeticResponse(args.id, args.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["apologetics", "detail"] });
    },
  });
}

export function useDeleteApologeticResponse() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteApologeticResponse(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["apologetics", "detail"] });
    },
  });
}
