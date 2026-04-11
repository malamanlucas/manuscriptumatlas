"use client";

import { useParams } from "next/navigation";
import { useTranslations, useLocale } from "next-intl";
import { Header } from "@/components/layout/Header";
import { StrongsEntry } from "@/components/bible/StrongsEntry";
import { StrongsConcordance } from "@/components/bible/StrongsConcordance";
import { useStrongsConcordance, useLexiconEntry } from "@/hooks/useBible";
import { useState } from "react";

export default function StrongsPage() {
  const params = useParams();
  const number = params.number as string;
  const t = useTranslations("bible");
  const locale = useLocale();
  const [page, setPage] = useState(1);
  const concordanceQuery = useStrongsConcordance(number, page);
  const lexiconQuery = useLexiconEntry(number, locale);

  return (
    <div className="min-h-screen">
      <Header
        title={`Strong's ${number.toUpperCase()}`}
        subtitle={t("strongsSubtitle")}
      />
      <div className="mx-auto w-full max-w-5xl p-4 md:p-6 space-y-4">
        {lexiconQuery.data && (
          <StrongsEntry entry={lexiconQuery.data} />
        )}
        <StrongsConcordance
          data={concordanceQuery.data}
          isLoading={concordanceQuery.isLoading}
          error={concordanceQuery.error}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}
