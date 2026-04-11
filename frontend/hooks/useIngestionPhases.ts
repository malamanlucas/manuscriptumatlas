import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getManuscriptIngestionPhases,
  runManuscriptPhase,
  runAllManuscriptPhases,
  getPatristicIngestionPhases,
  runPatristicPhase,
  runPatristicPhases,
  runAllPatristicPhases,
  getBibleIngestionPhases,
  runBiblePhase,
  runBiblePhases,
  runAllBiblePhases,
  clearBibleGlosses,
} from "@/lib/api";

// ── Manuscript Ingestion Phases ──

export function useManuscriptIngestionPhases() {
  return useQuery({
    queryKey: ["manuscript-ingestion", "phases"],
    queryFn: getManuscriptIngestionPhases,
    refetchInterval: (query) =>
      query.state.data?.some((p) => p.status === "running") ? 3000 : false,
    staleTime: 2_000,
  });
}

export function useRunManuscriptPhase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phase: string) => runManuscriptPhase(phase),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["manuscript-ingestion"] });
    },
  });
}

export function useRunAllManuscriptPhases() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => runAllManuscriptPhases(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["manuscript-ingestion"] });
    },
  });
}

// ── Patristic Ingestion Phases ──

export function usePatristicIngestionPhases() {
  return useQuery({
    queryKey: ["patristic-ingestion", "phases"],
    queryFn: getPatristicIngestionPhases,
    refetchInterval: (query) =>
      query.state.data?.some((p) => p.status === "running") ? 3000 : false,
    staleTime: 2_000,
  });
}

export function useRunPatristicPhase(filter?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phase: string) => runPatristicPhase(phase, filter),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["patristic-ingestion"] });
    },
  });
}

export function useRunPatristicPhases(filter?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phases: string[]) => runPatristicPhases(phases, filter),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["patristic-ingestion"] });
    },
  });
}

export function useRunAllPatristicPhases(filter?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => runAllPatristicPhases(filter),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["patristic-ingestion"] });
    },
  });
}

// ── Bible Ingestion Phases ──

export function useBibleIngestionPhases() {
  return useQuery({
    queryKey: ["bible-ingestion", "phases"],
    queryFn: getBibleIngestionPhases,
    refetchInterval: (query) =>
      query.state.data?.some((p) => p.status === "running") ? 3000 : false,
    staleTime: 2_000,
  });
}

export function useRunBiblePhase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phase: string) => runBiblePhase(phase),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bible-ingestion"] });
    },
  });
}

export function useRunBibleLayerPhases() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (phases: string[]) => runBiblePhases(phases),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bible-ingestion"] });
    },
  });
}

export function useRunAllBiblePhases() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => runAllBiblePhases(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bible-ingestion"] });
    },
  });
}

export function useClearBibleGlosses() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => clearBibleGlosses(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bible-ingestion"] });
    },
  });
}
