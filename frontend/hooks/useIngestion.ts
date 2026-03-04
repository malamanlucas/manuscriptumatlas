import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getIngestionStatus,
  triggerIngestion,
  resetAndReIngest,
  triggerDatingEnrichment,
} from "@/lib/api";

export function useIngestionStatus() {
  const query = useQuery({
    queryKey: ["admin", "ingestion", "status"],
    queryFn: getIngestionStatus,
    refetchInterval: (query) => {
      const data = query.state.data;
      return data?.isRunning ? 5000 : false;
    },
  });
  return query;
}

export function useTriggerIngestion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: triggerIngestion,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["admin", "ingestion", "status"],
      });
    },
  });
}

export function useResetAndReIngest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: resetAndReIngest,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["admin", "ingestion", "status"],
      });
    },
  });
}

export function useDatingEnrichment() {
  return useMutation({
    mutationFn: ({ domain, limit }: { domain: "fathers" | "manuscripts" | "all"; limit: number }) =>
      triggerDatingEnrichment(domain, limit),
  });
}
