import { useQuery } from "@tanstack/react-query";
import { getMissingVerses } from "@/lib/api";

export function useMissingVerses(
  book: string,
  century: number,
  type?: string
) {
  return useQuery({
    queryKey: ["missing", book, century, type],
    queryFn: () => getMissingVerses(book, century, type),
    enabled: !!book,
    staleTime: 5 * 60 * 1000,
  });
}
