"use client";

import { useTranslations } from "next-intl";

const dotColors: Record<string, string> = {
  HIGH: "bg-emerald-500",
  MEDIUM: "bg-amber-500",
  LOW: "bg-red-500",
};

interface ConfidenceDotProps {
  confidence?: string | null;
  source?: string | null;
}

export function ConfidenceDot({ confidence, source }: ConfidenceDotProps) {
  const t = useTranslations("dating");
  const level = confidence?.toUpperCase() ?? "";
  const color = dotColors[level];
  if (!color) return null;

  const levelKey = level.toLowerCase() as "high" | "medium" | "low";
  const tooltip = t("confidenceTooltip", {
    source: source ?? "",
    level: t(levelKey),
  });

  return (
    <span
      title={tooltip}
      className={`inline-block h-1.5 w-1.5 shrink-0 rounded-full ${color}`}
    />
  );
}
