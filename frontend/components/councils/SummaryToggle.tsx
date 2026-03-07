"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";

export function SummaryToggle({
  summary,
  originalText,
  shortDescription,
}: {
  summary?: string | null;
  originalText?: string | null;
  shortDescription?: string | null;
}) {
  const t = useTranslations("councils");
  const [showOriginal, setShowOriginal] = useState(false);

  const hasSummary = !!summary?.trim();
  const hasOriginal = !!originalText?.trim();
  const hasShortDescription = !!shortDescription?.trim();

  if (!hasSummary && !hasOriginal && !hasShortDescription) {
    return <p className="text-sm text-muted-foreground">{t("noTextAvailable")}</p>;
  }

  const displayContent = hasSummary || hasOriginal
    ? (showOriginal ? originalText : summary ?? originalText)
    : shortDescription;
  const isShortDescriptionFallback = !hasSummary && !hasOriginal && hasShortDescription;

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
      {isShortDescriptionFallback && (
        <span className="inline-block rounded px-2 py-0.5 text-xs font-medium text-muted-foreground border border-border">
          {t("shortDescriptionBadge")}
        </span>
      )}
      <div className="prose prose-sm max-w-none dark:prose-invert whitespace-pre-wrap rounded-lg border border-border p-4">
        {displayContent}
      </div>
    </div>
  );
}
