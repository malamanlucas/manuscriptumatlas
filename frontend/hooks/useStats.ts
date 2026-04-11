import { useQuery } from "@tanstack/react-query";
import { getStatsOverview, getManuscriptsCount } from "@/lib/api";

export function useStatsOverview() {
  return useQuery({
    queryKey: ["stats", "overview"],
    queryFn: getStatsOverview,
    staleTime: 5 * 60 * 1000,
  });
}

export function useManuscriptsCount() {
  return useQuery({
    queryKey: ["stats", "manuscripts-count"],
    queryFn: getManuscriptsCount,
    staleTime: 5 * 60 * 1000,
  });
}
