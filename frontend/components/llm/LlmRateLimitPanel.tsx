"use client";

import { useTranslations } from "next-intl";
import { Gauge } from "lucide-react";
import type { RateLimiterStatusDTO } from "@/types";

function StatusDot({ value, max }: { value?: number; max?: number }) {
  if (value == null) return <span className="h-2.5 w-2.5 rounded-full bg-gray-400" />;
  const pct = max ? (value / max) * 100 : value > 100 ? 100 : value;
  const color = pct > 50 ? "bg-emerald-500" : pct > 20 ? "bg-amber-500" : "bg-red-500";
  return <span className={`h-2.5 w-2.5 rounded-full ${color}`} />;
}

export function LlmRateLimitPanel({ statuses }: { statuses: RateLimiterStatusDTO[] }) {
  const t = useTranslations("llmUsage.rateLimiter");

  return (
    <div className="rounded-xl border border-border bg-card p-4 md:p-5">
      <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold">
        <Gauge className="h-4 w-4" />
        {t("title")}
      </h3>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {statuses.map((s) => (
          <div key={s.provider} className="rounded-lg bg-muted/50 p-3">
            <p className="mb-2 text-xs font-semibold">{s.provider}</p>
            <div className="space-y-1.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">{t("remainingRequests")}</span>
                <span className="flex items-center gap-1.5 font-medium">
                  <StatusDot value={s.remainingRequests} />
                  {s.remainingRequests ?? t("unknown")}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">{t("remainingTokens")}</span>
                <span className="flex items-center gap-1.5 font-medium">
                  <StatusDot value={s.remainingTokens} />
                  {s.remainingTokens != null
                    ? s.remainingTokens.toLocaleString()
                    : t("unknown")}
                </span>
              </div>
              {s.resetTime && (
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">{t("resetTime")}</span>
                  <span className="font-medium">{s.resetTime}</span>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
