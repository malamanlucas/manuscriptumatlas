"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";

export function SummaryToggle({
  summary,
  originalText,
}: {
  summary?: string | null;
  originalText?: string | null;
}) {
  const t = useTranslations("councils");
  const [showOriginal, setShowOriginal] = useState(false);

  const hasSummary = !!summary?.trim();
  const hasOriginal = !!originalText?.trim();

  if (!hasSummary && !hasOriginal) {
    return <p className="text-sm text-muted-foreground">{t("noTextAvailable")}</p>;
  }

  return (
    <div className="space-y-3">
      {hasSummary && hasOriginal && (
        <div className="inline-flex rounded-lg border border-border bg-background p-1">
          <button
            onClick={() => setShowOriginal(false)}
            className={`rounded-md px-3 py-1 text-xs font-medium ${!showOriginal ? "bg-primary text-primary-foreground" : "text-muted-foreground"}`}
          >
            {t("summary")}
          </button>
          <button
            onClick={() => setShowOriginal(true)}
            className={`rounded-md px-3 py-1 text-xs font-medium ${showOriginal ? "bg-primary text-primary-foreground" : "text-muted-foreground"}`}
          >
            {t("original")}
          </button>
        </div>
      )}
      <div className="prose prose-sm max-w-none dark:prose-invert whitespace-pre-wrap rounded-lg border border-border p-4">
        {showOriginal ? originalText : summary ?? originalText}
      </div>
    </div>
  );
}
