import { useQuery } from "@tanstack/react-query";
import {
  getCoverageByCentury,
  getChapterCoverage,
  getGospelCoverage,
} from "@/lib/api";

export function useCoverageByCentury(century: number, type?: string) {
  return useQuery({
    queryKey: ["coverage", "century", century, type],
    queryFn: () => getCoverageByCentury(century, type),
    staleTime: 5 * 60 * 1000,
  });
}

export function useChapterCoverage(
  book: string,
  century: number,
  type?: string
) {
  return useQuery({
    queryKey: ["coverage", "chapters", book, century, type],
    queryFn: () => getChapterCoverage(book, century, type),
    enabled: !!book,
    staleTime: 5 * 60 * 1000,
  });
}

export function useGospelCoverage(century: number, type?: string) {
  return useQuery({
    queryKey: ["coverage", "gospels", century, type],
    queryFn: () => getGospelCoverage(century, type),
    staleTime: 5 * 60 * 1000,
  });
}
