"use client";

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
  Legend,
} from "recharts";
import { toRoman } from "@/lib/utils";
import { useTranslations } from "next-intl";
import type { ChurchFatherSummary } from "@/types";

const TRADITION_COLORS: Record<string, string> = {
  greek: "#3b82f6",
  latin: "#f59e0b",
  syriac: "#10b981",
  coptic: "#8b5cf6",
};

interface FathersTimelineChartProps {
  fathers: ChurchFatherSummary[];
  onBarClick?: (century: number) => void;
}

export function FathersTimelineChart({
  fathers,
  onBarClick,
}: FathersTimelineChartProps) {
  const t = useTranslations("fathers");

  const data = fathers
    .sort((a, b) => a.centuryMin - b.centuryMin || a.centuryMax - b.centuryMax)
    .map((f) => ({
      name: f.displayName,
      range: [f.centuryMin, f.centuryMax + 0.8],
      tradition: f.tradition,
      centuryMin: f.centuryMin,
      centuryMax: f.centuryMax,
      location: f.primaryLocation,
    }));

  const chartHeight = Math.max(300, data.length * 36 + 80);

  return (
    <div>
      <div className="overflow-x-auto">
        <div style={{ minWidth: 600 }}>
          <ResponsiveContainer width="100%" height={chartHeight}>
            <BarChart
              data={data}
              layout="vertical"
              margin={{ top: 10, right: 30, left: 10, bottom: 10 }}
              onClick={(state) => {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                const payload = (state as any)?.activePayload;
                if (payload?.[0] && onBarClick) {
                  onBarClick(payload[0].payload.centuryMin);
                }
              }}
            >
              <CartesianGrid strokeDasharray="3 3" opacity={0.2} horizontal={false} />
              <XAxis
                type="number"
                domain={[0.5, 10.8]}
                ticks={[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}
                tickFormatter={(v) => toRoman(Math.round(v))}
                tick={{ fontSize: 12 }}
              />
              <YAxis
                type="category"
                dataKey="name"
                width={140}
                tick={{ fontSize: 11 }}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: "var(--card)",
                  border: "1px solid var(--border)",
                  borderRadius: "8px",
                }}
                content={({ active, payload }) => {
                  if (!active || !payload?.length) return null;
                  const d = payload[0].payload;
                  return (
                    <div className="rounded-lg border border-border bg-card p-3 text-sm shadow-lg">
                      <p className="font-semibold">{d.name}</p>
                      <p className="text-muted-foreground">
                        {t("centuries")}: {toRoman(d.centuryMin)}
                        {d.centuryMax !== d.centuryMin && `–${toRoman(d.centuryMax)}`}
                      </p>
                      <p className="text-muted-foreground">
                        {t("tradition")}: {t(`traditions.${d.tradition}`)}
                      </p>
                      {d.location && (
                        <p className="text-muted-foreground">
                          {t("location")}: {d.location}
                        </p>
                      )}
                    </div>
                  );
                }}
              />
              <Legend
                content={() => (
                  <div className="mt-2 flex flex-wrap justify-center gap-4 text-xs">
                    {Object.entries(TRADITION_COLORS).map(([key, color]) => (
                      <div key={key} className="flex items-center gap-1.5">
                        <div
                          className="h-3 w-3 rounded-sm"
                          style={{ backgroundColor: color }}
                        />
                        <span className="text-muted-foreground">
                          {t(`traditions.${key}`)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              />
              <Bar dataKey="range" radius={[0, 4, 4, 0]} cursor="pointer">
                {data.map((entry, index) => (
                  <Cell
                    key={index}
                    fill={TRADITION_COLORS[entry.tradition] ?? "#6b7280"}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
      <p className="mt-1 text-center text-xs text-muted-foreground md:hidden">
        ← {t("scrollHint")} →
      </p>
    </div>
  );
}
