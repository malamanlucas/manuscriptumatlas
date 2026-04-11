"use client";

import { useState, useCallback } from "react";
import { useTranslations } from "next-intl";
import { useLocale } from "next-intl";
import { Header } from "@/components/layout/Header";
import { BibleSearchBar } from "@/components/bible/BibleSearchBar";
import { BibleSearchResults } from "@/components/bible/BibleSearchResults";
import { useBibleSearch, useBibleVersions } from "@/hooks/useBible";

export default function SearchPage() {
  const t = useTranslations("bible");
  const locale = useLocale();
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [version, setVersion] = useState<string | undefined>();
  const [testament, setTestament] = useState<string | undefined>();
  const [page, setPage] = useState(1);

  const versionsQuery = useBibleVersions();
  const searchQuery = useBibleSearch(debouncedQuery, { version, testament, locale, page });

  const handleSearch = useCallback((q: string) => {
    setQuery(q);
    setPage(1);
    // Simple debounce via timeout
    const timer = setTimeout(() => setDebouncedQuery(q), 300);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="min-h-screen">
      <Header title={t("searchTitle")} subtitle={t("searchSubtitle")} />
      <div className="mx-auto w-full max-w-5xl p-4 md:p-6 space-y-4">
        <BibleSearchBar
          query={query}
          onQueryChange={handleSearch}
          version={version}
          onVersionChange={setVersion}
          testament={testament}
          onTestamentChange={setTestament}
          versions={versionsQuery.data ?? []}
        />

        {debouncedQuery.length >= 2 && (
          <BibleSearchResults
            data={searchQuery.data}
            isLoading={searchQuery.isLoading}
            error={searchQuery.error}
            onPageChange={setPage}
          />
        )}
      </div>
    </div>
  );
}
