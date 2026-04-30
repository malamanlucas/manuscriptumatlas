import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { getBibleVersions, getBibleBooks, getBibleChapter, getBibleInterlinearChapter, getBibleInterlinearVerse, getStrongsConcordance, getLexiconEntry, compareBibleChapter, searchBible } from "@/lib/api";

export function useBibleVersions(testament?: string) {
  return useQuery({
    queryKey: ["bible", "versions", testament],
    queryFn: () => getBibleVersions(testament),
    staleTime: 60_000,
  });
}

export function useBibleBooks(testament?: string) {
  const locale = useLocale();
  return useQuery({
    queryKey: ["bible", "books", testament, locale],
    queryFn: () => getBibleBooks(testament, locale),
    staleTime: 60_000,
  });
}

export function useBibleChapter(version: string, book: string, chapter: number) {
  return useQuery({
    queryKey: ["bible", "chapter", version, book, chapter],
    queryFn: () => getBibleChapter(version, book, chapter),
    enabled: !!version && !!book && chapter > 0,
    staleTime: 300_000,
  });
}

export function useBibleInterlinearChapter(book: string, chapter: number, alignVersion?: string) {
  return useQuery({
    queryKey: ["bible", "interlinear", book, chapter, alignVersion],
    queryFn: () => getBibleInterlinearChapter(book, chapter, alignVersion),
    enabled: !!book && chapter > 0,
    staleTime: 300_000,
  });
}

export function useBibleInterlinearVerse(book: string, chapter: number, verse: number, alignVersion?: string) {
  return useQuery({
    queryKey: ["bible", "interlinear", book, chapter, verse, alignVersion],
    queryFn: () => getBibleInterlinearVerse(book, chapter, verse, alignVersion),
    enabled: !!book && chapter > 0 && verse > 0,
    staleTime: 300_000,
  });
}

export function useStrongsConcordance(strongsNumber: string, page: number = 1) {
  return useQuery({
    queryKey: ["bible", "strongs", strongsNumber, page],
    queryFn: () => getStrongsConcordance(strongsNumber, page),
    enabled: !!strongsNumber,
    staleTime: 300_000,
  });
}

export function useLexiconEntry(strongsNumber: string, locale: string = "en") {
  return useQuery({
    queryKey: ["bible", "lexicon", strongsNumber, locale],
    queryFn: () => getLexiconEntry(strongsNumber, locale),
    enabled: !!strongsNumber,
    staleTime: 300_000,
  });
}

export function useBibleCompare(book: string, chapter: number, versions: string[]) {
  return useQuery({
    queryKey: ["bible", "compare", book, chapter, versions.join(",")],
    queryFn: () => compareBibleChapter(book, chapter, versions),
    enabled: !!book && chapter > 0 && versions.length >= 2,
    staleTime: 300_000,
  });
}

export function useBibleSearch(query: string, options?: { version?: string; testament?: string; book?: string; locale?: string; page?: number }) {
  return useQuery({
    queryKey: ["bible", "search", query, options],
    queryFn: () => searchBible(query, options),
    enabled: query.length >= 2,
    staleTime: 10_000,
  });
}
