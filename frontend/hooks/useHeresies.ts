import { useQuery } from "@tanstack/react-query";
import { getHeresies, getHeresyDetail, getHeresyCouncils } from "@/lib/api";

export function useHeresies(params?: { page?: number; limit?: number; locale?: string }) {
  return useQuery({
    queryKey: ["heresies", "list", params],
    queryFn: () => getHeresies(params),
    staleTime: 30_000,
  });
}

export function useHeresyDetail(slug: string, locale?: string) {
  return useQuery({
    queryKey: ["heresies", "detail", slug, locale],
    queryFn: () => getHeresyDetail(slug, locale),
    enabled: !!slug,
    staleTime: 30_000,
  });
}

export function useHeresyCouncils(slug: string, locale?: string) {
  return useQuery({
    queryKey: ["heresies", slug, "councils", locale],
    queryFn: () => getHeresyCouncils(slug, locale),
    enabled: !!slug,
    staleTime: 30_000,
  });
}
