import { useQuery } from "@tanstack/react-query";
import { getNtMetrics, getBookMetrics } from "@/lib/api";

export function useNtMetrics() {
  return useQuery({
    queryKey: ["metrics", "nt"],
    queryFn: getNtMetrics,
    staleTime: 5 * 60 * 1000,
  });
}

export function useBookMetrics(book: string | null) {
  return useQuery({
    queryKey: ["metrics", "book", book],
    queryFn: () => getBookMetrics(book!),
    enabled: !!book,
    staleTime: 5 * 60 * 1000,
  });
}
