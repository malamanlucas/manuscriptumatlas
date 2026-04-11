"use client";

import { useTheme } from "next-themes";
import { useTranslations } from "next-intl";
import { Sun, Moon, Monitor } from "lucide-react";
import { useEffect, useState } from "react";

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const t = useTranslations("theme");
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  if (!mounted) {
    return (
      <div className="flex items-center gap-1.5">
        <Monitor className="h-3.5 w-3.5 text-white/50" />
        <div className="flex gap-0.5">
          {["light", "dark", "system"].map((v) => (
            <span
              key={v}
              className="rounded px-1.5 py-0.5 text-xs font-medium text-white/50"
            >
              {v === "light" ? "☀" : v === "dark" ? "☾" : "⚙"}
            </span>
          ))}
        </div>
      </div>
    );
  }

  const options = [
    { value: "light" as const, icon: Sun, label: t("light") },
    { value: "dark" as const, icon: Moon, label: t("dark") },
    { value: "system" as const, icon: Monitor, label: t("system") },
  ];

  return (
    <div className="flex items-center gap-1.5">
      {options.map(({ value, icon: Icon, label }) => (
        <button
          key={value}
          onClick={() => setTheme(value)}
          title={label}
          className={`rounded px-1.5 py-0.5 text-xs font-medium transition-colors ${
            theme === value
              ? "bg-white/20 text-white"
              : "text-white/50 hover:text-white/80"
          }`}
        >
          <Icon className="h-3.5 w-3.5" />
        </button>
      ))}
    </div>
  );
}
