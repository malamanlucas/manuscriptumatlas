import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  runBibleScopedPhases,
  getBibleLayer4Coverage,
  getBibleLayer4Applications,
} from "@/lib/api";
import type { RunScopedRequest } from "@/types";

export function useRunBibleScopedPhases() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: RunScopedRequest) => runBibleScopedPhases(request),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bible-ingestion"] });
      qc.invalidateQueries({ queryKey: ["bible-layer4"] });
    },
    onError: (error: Error) => {
      console.error("[useRunBibleScopedPhases] failed:", error);
      alert(`Erro ao executar fases com escopo: ${error.message}`);
    },
  });
}

export function useBibleLayer4Coverage(book: string | null, chapter: number | null) {
  return useQuery({
    queryKey: ["bible-layer4", "coverage", book, chapter],
    queryFn: () => getBibleLayer4Coverage(book!, chapter!),
    enabled: !!book && chapter != null && chapter > 0,
    staleTime: 5_000,
  });
}

export function useBibleLayer4Applications(options: {
  book?: string | null;
  chapter?: number | null;
  verse?: number | null;
  limit?: number;
}) {
  return useQuery({
    queryKey: ["bible-layer4", "applications", options.book, options.chapter, options.verse, options.limit],
    queryFn: () =>
      getBibleLayer4Applications({
        book: options.book ?? undefined,
        chapter: options.chapter ?? undefined,
        verse: options.verse ?? undefined,
        limit: options.limit ?? 20,
      }),
    refetchInterval: (query) =>
      query.state.data?.some((a) => a.status === "running") ? 3000 : false,
    staleTime: 2_000,
  });
}
