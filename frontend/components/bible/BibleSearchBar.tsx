"use client";

import { useTranslations } from "next-intl";
import { Search } from "lucide-react";
import type { BibleVersionDTO } from "@/types";

interface BibleSearchBarProps {
  query: string;
  onQueryChange: (q: string) => void;
  version?: string;
  onVersionChange: (v: string | undefined) => void;
  testament?: string;
  onTestamentChange: (t: string | undefined) => void;
  versions: BibleVersionDTO[];
}

export function BibleSearchBar({
  query,
  onQueryChange,
  version,
  onVersionChange,
  testament,
  onTestamentChange,
  versions,
}: BibleSearchBarProps) {
  const t = useTranslations("bible");

  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
          placeholder={t("searchPlaceholder")}
          className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
        />
      </div>
      <div className="mt-3 flex flex-wrap gap-3">
        <select
          value={version ?? ""}
          onChange={(e) => onVersionChange(e.target.value || undefined)}
          className="rounded-lg border border-border bg-background px-3 py-1.5 text-xs"
        >
          <option value="">{t("allVersions")}</option>
          {versions.map((v) => (
            <option key={v.code} value={v.code}>{v.code}</option>
          ))}
        </select>
        <select
          value={testament ?? ""}
          onChange={(e) => onTestamentChange(e.target.value || undefined)}
          className="rounded-lg border border-border bg-background px-3 py-1.5 text-xs"
        >
          <option value="">{t("allTestaments")}</option>
          <option value="OT">{t("oldTestament")}</option>
          <option value="NT">{t("newTestament")}</option>
        </select>
      </div>
    </div>
  );
}
