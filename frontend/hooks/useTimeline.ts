import { useQuery } from "@tanstack/react-query";
import { getTimeline, getTimelineFull } from "@/lib/api";

export function useTimeline(book?: string, type?: string) {
  return useQuery({
    queryKey: ["timeline", book, type],
    queryFn: () => (book ? getTimeline(book, type) : getTimelineFull(type)),
    staleTime: 5 * 60 * 1000,
  });
}
