import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getCouncilIngestionPhases,
  runCouncilPhase,
  runCouncilPhases,
  runAllCouncilPhases,
  getCouncilIngestionCache,
} from "@/lib/api";

export function useCouncilIngestionPhases() {
  return useQuery({
    queryKey: ["council-ingestion", "phases"],
    queryFn: getCouncilIngestionPhases,
    refetchInterval: (query) =>
      query.state.data?.some((p) => p.status === "running") ? 3000 : false,
    staleTime: 2_000,
  });
}

export function useCouncilIngestionCache() {
  return useQuery({
    queryKey: ["council-ingestion", "cache"],
    queryFn: getCouncilIngestionCache,
    staleTime: 10_000,
  });
}

export function useRunCouncilPhase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phase: string) => runCouncilPhase(phase),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["council-ingestion"] });
    },
  });
}

export function useRunCouncilPhases() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phases: string[]) => runCouncilPhases(phases),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["council-ingestion"] });
    },
  });
}

export function useRunAllCouncilPhases() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => runAllCouncilPhases(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["council-ingestion"] });
    },
  });
}
