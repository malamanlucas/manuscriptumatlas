"use client";

import { useLocale } from "next-intl";
import { useRouter, usePathname } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";
import { Globe } from "lucide-react";

const LOCALE_LABELS: Record<string, string> = {
  en: "EN",
  pt: "PT",
  es: "ES",
};

export function LanguageSelector() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();

  function switchLocale(newLocale: string) {
    router.replace(pathname, { locale: newLocale });
  }

  return (
    <div className="flex items-center gap-1.5">
      <Globe className="h-3.5 w-3.5 text-white/50" />
      {routing.locales.map((l) => (
        <button
          key={l}
          onClick={() => switchLocale(l)}
          className={`rounded px-1.5 py-0.5 text-xs font-medium transition-colors ${
            locale === l
              ? "bg-white/20 text-white"
              : "text-white/50 hover:text-white/80"
          }`}
        >
          {LOCALE_LABELS[l]}
        </button>
      ))}
    </div>
  );
}
