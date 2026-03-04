"use client";

import { useTranslations } from "next-intl";

interface DatingBadgeProps {
  yearMin?: number | null;
  yearMax?: number | null;
  yearBest?: number | null;
  datingSource?: string | null;
  datingConfidence?: string | null;
  datingReference?: string | null;
}

const confidenceStyles: Record<string, string> = {
  HIGH: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
  MEDIUM: "bg-amber-500/15 text-amber-400 border-amber-500/30",
  LOW: "bg-red-500/15 text-red-400 border-red-500/30",
};

export function DatingBadge({
  yearMin,
  yearMax,
  yearBest,
  datingSource,
  datingConfidence,
  datingReference,
}: DatingBadgeProps) {
  const t = useTranslations("dating");

  const hasYear = yearBest != null || yearMin != null || yearMax != null;
  if (!hasYear) return null;

  const dateLabel = yearBest
    ? `c. ${yearBest} AD`
    : `${yearMin ?? "?"}–${yearMax ?? "?"} AD`;

  const confidence = datingConfidence?.toUpperCase() ?? "";
  const badgeStyle = confidenceStyles[confidence] ?? confidenceStyles.MEDIUM;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-2 flex-wrap">
        <span className="text-sm font-semibold text-foreground">{dateLabel}</span>

        {confidence && (
          <span
            className={`inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium ${badgeStyle}`}
          >
            {t(confidence.toLowerCase() as "high" | "medium" | "low")}
          </span>
        )}

        {confidence === "LOW" && (
          <span className="text-[11px] text-red-400/80">{t("aiGenerated")}</span>
        )}

        {datingSource && (
          <span className="text-[11px] text-muted-foreground">
            {t("source")}: {datingSource}
          </span>
        )}
      </div>

      {datingReference && (
        <span className="text-[11px] text-muted-foreground leading-tight">
          {datingReference}
        </span>
      )}
    </div>
  );
}
