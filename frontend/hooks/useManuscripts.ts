import { useQuery } from "@tanstack/react-query";
import { getManuscripts, getManuscriptDetail } from "@/lib/api";

export function useManuscripts(params?: {
  type?: string;
  century?: number;
  page?: number;
  limit?: number;
}) {
  return useQuery({
    queryKey: ["manuscripts", params],
    queryFn: () => getManuscripts(params),
    staleTime: 5 * 60 * 1000,
  });
}

export function useManuscriptDetail(gaId: string | null) {
  return useQuery({
    queryKey: ["manuscript", gaId],
    queryFn: () => getManuscriptDetail(gaId!),
    enabled: !!gaId,
    staleTime: 5 * 60 * 1000,
  });
}
