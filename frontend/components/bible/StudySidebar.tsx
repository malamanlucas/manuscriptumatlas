"use client";

import { useTranslations } from "next-intl";
import { BookOpen, X } from "lucide-react";

interface StudySidebarProps {
  interlinearMode: boolean;
  onToggleInterlinear: () => void;
  hasAlignmentData: boolean;
  alignVersion: string;
  onAlignVersionChange: (version: string) => void;
  primaryLang: string;
  onClose?: () => void;
}

const ALIGN_OPTIONS = [
  { value: "KJV", label: "KJV — English", lang: "en" },
  { value: "ARC69", label: "ARC69 — Português", lang: "pt" },
];

export function StudySidebar({
  interlinearMode,
  onToggleInterlinear,
  hasAlignmentData,
  alignVersion,
  onAlignVersionChange,
  primaryLang,
  onClose,
}: StudySidebarProps) {
  const t = useTranslations("bible");
  const filteredOptions = ALIGN_OPTIONS.filter((opt) => opt.lang === primaryLang);

  return (
    <div className="rounded-xl border border-border bg-card p-4 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <BookOpen className="h-4 w-4 text-primary" />
          <h3 className="text-sm font-semibold">{t("studyTools")}</h3>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className="rounded-md p-1 text-muted-foreground hover:bg-muted transition-colors lg:hidden"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* Interlinear toggle */}
      <div className="space-y-2">
        <button
          onClick={onToggleInterlinear}
          className={`w-full flex items-center justify-between rounded-lg border px-3 py-2.5 text-sm font-medium transition-colors ${
            interlinearMode
              ? "border-primary bg-primary/10 text-primary"
              : "border-border text-muted-foreground hover:border-primary/50 hover:text-foreground"
          }`}
        >
          <span>{t("interlinearMode")}</span>
          <kbd className="hidden sm:inline-flex items-center rounded border border-border bg-muted px-1.5 py-0.5 text-[10px] font-mono text-muted-foreground">
            Ctrl+I
          </kbd>
        </button>
      </div>

      {/* Alignment version selector */}
      <div className="rounded-lg border border-border/50 bg-muted/20 px-3 py-2.5 space-y-2">
        <p className="text-xs font-medium text-muted-foreground">{t("alignmentVersion")}</p>
        <select
          value={alignVersion}
          onChange={(e) => onAlignVersionChange(e.target.value)}
          className="w-full rounded-md border border-border bg-background px-2 py-1.5 text-xs font-medium focus:outline-none focus:ring-1 focus:ring-primary/50"
        >
          {filteredOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
        <div className="flex items-center gap-2">
          <span
            className={`inline-block h-2 w-2 rounded-full ${
              hasAlignmentData ? "bg-emerald-500" : "bg-muted-foreground/30"
            }`}
          />
          <span className="text-xs text-foreground/70">
            {hasAlignmentData ? t("aligned") : t("notAligned")}
          </span>
        </div>
      </div>

      {/* Color legend */}
      {interlinearMode && (
        <div className="rounded-lg border border-border/50 bg-muted/20 px-3 py-2.5 space-y-2">
          <p className="text-xs font-medium text-muted-foreground">{t("colorLegend")}</p>
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="inline-block h-2.5 w-2.5 rounded-sm bg-blue-500/30 border border-blue-500/50" />
              <span className="text-xs text-foreground/70">{t("matchingGloss")}</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-block h-2.5 w-2.5 rounded-sm bg-amber-500/30 border border-amber-500/50" />
              <span className="text-xs text-foreground/70">{t("divergentGloss")}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
