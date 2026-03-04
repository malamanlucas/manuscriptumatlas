"use client";

import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer, Legend } from "recharts";
import type { CouncilSummaryDTO } from "@/types";
import { useTranslations } from "next-intl";
import { toRoman } from "@/lib/utils";

type Props = {
  councils: CouncilSummaryDTO[];
  onCenturyClick?: (century: number) => void;
};

export function CouncilsTimelineChart({ councils, onCenturyClick }: Props) {
  const t = useTranslations("councils");

  const grouped = new Map<number, { century: number; ecumenical: number; regional: number; local: number }>();
  for (const council of councils) {
    const bucket = grouped.get(council.century) ?? { century: council.century, ecumenical: 0, regional: 0, local: 0 };
    if (council.councilType === "ECUMENICAL") bucket.ecumenical += 1;
    else if (council.councilType === "REGIONAL") bucket.regional += 1;
    else bucket.local += 1;
    grouped.set(council.century, bucket);
  }

  const data = Array.from(grouped.values()).sort((a, b) => a.century - b.century);

  return (
    <div className="h-[320px] w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={data}
          margin={{ top: 16, right: 16, left: 0, bottom: 8 }}
          onClick={(state) => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const payload = (state as any)?.activePayload?.[0]?.payload;
            if (payload?.century && onCenturyClick) {
              onCenturyClick(payload.century);
            }
          }}
        >
          <CartesianGrid strokeDasharray="3 3" opacity={0.25} />
          <XAxis dataKey="century" tickFormatter={(v) => toRoman(v)} />
          <YAxis allowDecimals={false} />
          <Tooltip
            formatter={(value, name) => [value ?? 0, t(`types.${String(name)}` as "types.ecumenical")]}
            labelFormatter={(label) => `${t("century")} ${toRoman(Number(label))}`}
          />
          <Legend />
          <Bar dataKey="ecumenical" stackId="a" fill="#f59e0b" />
          <Bar dataKey="regional" stackId="a" fill="#3b82f6" />
          <Bar dataKey="local" stackId="a" fill="#6b7280" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
