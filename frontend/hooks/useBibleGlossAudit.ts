import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  auditBibleGlosses,
  getBibleGlossAuditStats,
  fixFlaggedBibleGlosses,
} from "@/lib/api";

export function useBibleGlossAuditStats(book?: string, chapter?: number) {
  return useQuery({
    queryKey: ["bible-gloss-audit", "stats", book ?? "all", chapter ?? "all"],
    queryFn: () => getBibleGlossAuditStats(book, chapter),
    refetchInterval: (query) =>
      query.state.data && query.state.data.pending > 0 ? 5000 : false,
    staleTime: 3_000,
  });
}

export function useAuditBibleGlosses() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ book, chapter }: { book?: string; chapter?: number }) =>
      auditBibleGlosses(book, chapter),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bible-gloss-audit"] });
      qc.invalidateQueries({ queryKey: ["llm-queue"] });
      qc.invalidateQueries({ queryKey: ["bible-ingestion"] });
    },
  });
}

export function useFixFlaggedBibleGlosses() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ book, chapter }: { book?: string; chapter?: number }) =>
      fixFlaggedBibleGlosses(book, chapter),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["bible-gloss-audit"] });
      qc.invalidateQueries({ queryKey: ["llm-queue"] });
      qc.invalidateQueries({ queryKey: ["bible-ingestion"] });
    },
  });
}
