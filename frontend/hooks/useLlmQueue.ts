import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchLlmQueueStats, retryFailedQueueItems, clearQueuePhase, unstickProcessingItems } from "@/lib/api";

export function useLlmQueueStats() {
  return useQuery({
    queryKey: ["llm-queue", "stats"],
    queryFn: fetchLlmQueueStats,
    refetchInterval: (query) => {
      const data = query.state.data;
      return (data?.totalPending ?? 0) > 0 || (data?.totalProcessing ?? 0) > 0
        ? 5000
        : false;
    },
    staleTime: 2_000,
  });
}

export function useRetryFailedQueue() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phase?: string) => retryFailedQueueItems(phase),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["llm-queue"] });
    },
  });
}

export function useUnstickProcessing() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phase?: string) => unstickProcessingItems(phase),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["llm-queue"] });
    },
  });
}

export function useClearQueuePhase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phase: string) => clearQueuePhase(phase),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["llm-queue"] });
    },
  });
}
