import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getIngestionStatus,
  triggerIngestion,
  resetAndReIngest,
  triggerDatingEnrichment,
  resetDomain,
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

export function useResetDomain() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (domain: "manuscripts" | "patristic" | "councils" | "bible" | "bible-layer1" | "bible-layer2" | "bible-layer3" | "bible-layer4") => resetDomain(domain),
    onSuccess: async (_data, domain) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin", "ingestion"] }),
        queryClient.invalidateQueries({ queryKey: ["manuscript-ingestion"] }),
        queryClient.invalidateQueries({ queryKey: ["patristic-ingestion"] }),
        queryClient.invalidateQueries({ queryKey: ["council-ingestion"] }),
        queryClient.invalidateQueries({ queryKey: ["bible-ingestion"] }),
      ]);
      console.log(`[useResetDomain] reset ${domain} succeeded, queries invalidated`);
    },
    onError: (error, domain) => {
      console.error(`[useResetDomain] reset ${domain} failed:`, error);
      alert(`Erro ao resetar ${domain}: ${error instanceof Error ? error.message : "Erro desconhecido"}`);
    },
  });
}

export function useDatingEnrichment() {
  return useMutation({
    mutationFn: ({ domain, limit }: { domain: "fathers" | "manuscripts" | "all"; limit: number }) =>
      triggerDatingEnrichment(domain, limit),
  });
}
