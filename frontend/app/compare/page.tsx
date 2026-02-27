"use client";

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { ComparisonChart } from "@/components/charts/ComparisonChart";
import { useTimeline } from "@/hooks/useTimeline";
import { toRoman } from "@/lib/utils";

type Preset = "papyrus-vs-uncial" | "gospels-vs-epistles" | "custom";

export default function ComparePage() {
  const [preset, setPreset] = useState<Preset>("papyrus-vs-uncial");

  const { data: papyrusData } = useTimeline(undefined, "papyrus");
  const { data: uncialData } = useTimeline(undefined, "uncial");
  const { data: allData } = useTimeline(undefined, undefined);

  const { data: matthewData } = useTimeline("Matthew");
  const { data: markData } = useTimeline("Mark");
  const { data: lukeData } = useTimeline("Luke");
  const { data: johnData } = useTimeline("John");
  const { data: romansData } = useTimeline("Romans");
  const { data: hebrewsData } = useTimeline("Hebrews");
  const { data: revelationData } = useTimeline("Revelation");

  const presets = [
    { id: "papyrus-vs-uncial" as Preset, label: "Papyri vs Uncials" },
    { id: "gospels-vs-epistles" as Preset, label: "Gospels vs Epistles" },
    { id: "custom" as Preset, label: "Individual Books" },
  ];

  const getPapyrusVsUncialSeries = () => {
    const series = [];
    if (papyrusData)
      series.push({
        label: "Papyri",
        entries: papyrusData.entries,
        color: "#3b82f6",
      });
    if (uncialData)
      series.push({
        label: "Uncials",
        entries: uncialData.entries,
        color: "#ef4444",
      });
    if (allData)
      series.push({
        label: "Combined",
        entries: allData.entries,
        color: "#10b981",
      });
    return series;
  };

  const getGospelsVsEpistlesSeries = () => {
    const series = [];
    if (matthewData)
      series.push({
        label: "Matthew",
        entries: matthewData.entries,
        color: "#3b82f6",
      });
    if (markData)
      series.push({
        label: "Mark",
        entries: markData.entries,
        color: "#ef4444",
      });
    if (lukeData)
      series.push({
        label: "Luke",
        entries: lukeData.entries,
        color: "#10b981",
      });
    if (johnData)
      series.push({
        label: "John",
        entries: johnData.entries,
        color: "#f59e0b",
      });
    return series;
  };

  const getCustomSeries = () => {
    const series = [];
    if (romansData)
      series.push({
        label: "Romans",
        entries: romansData.entries,
        color: "#8b5cf6",
      });
    if (hebrewsData)
      series.push({
        label: "Hebrews",
        entries: hebrewsData.entries,
        color: "#ec4899",
      });
    if (revelationData)
      series.push({
        label: "Revelation",
        entries: revelationData.entries,
        color: "#14b8a6",
      });
    if (johnData)
      series.push({
        label: "John",
        entries: johnData.entries,
        color: "#f59e0b",
      });
    return series;
  };

  const series =
    preset === "papyrus-vs-uncial"
      ? getPapyrusVsUncialSeries()
      : preset === "gospels-vs-epistles"
        ? getGospelsVsEpistlesSeries()
        : getCustomSeries();

  const summaryData =
    preset === "papyrus-vs-uncial"
      ? [
          {
            label: "Papyri Only",
            data: papyrusData,
            color: "text-blue-500",
          },
          {
            label: "Uncials Only",
            data: uncialData,
            color: "text-red-500",
          },
          {
            label: "Combined",
            data: allData,
            color: "text-emerald-500",
          },
        ]
      : null;

  return (
    <div className="min-h-screen">
      <Header title="Comparative Analysis" subtitle="Side-by-side coverage comparison" />

      <div className="p-6 space-y-6">
        <div className="rounded-xl border border-border bg-card p-6">
          <div className="flex gap-2">
            {presets.map((p) => (
              <button
                key={p.id}
                onClick={() => setPreset(p.id)}
                className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
                  preset === p.id
                    ? "bg-primary text-primary-foreground"
                    : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>

        {series.length > 0 && (
          <div className="rounded-xl border border-border bg-card p-6">
            <h2 className="mb-4 text-base font-semibold">
              {preset === "papyrus-vs-uncial"
                ? "Papyri vs Uncials vs Combined"
                : preset === "gospels-vs-epistles"
                  ? "Four Gospels Comparison"
                  : "Selected Books Comparison"}
            </h2>
            <ComparisonChart series={series} />
          </div>
        )}

        {summaryData && (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {summaryData.map((item) => {
              const latest = item.data?.entries[item.data.entries.length - 1];
              const peakGrowth = item.data?.entries.reduce(
                (max, e) => (e.growthPercent > max.growthPercent ? e : max),
                item.data.entries[0]
              );
              return (
                <div
                  key={item.label}
                  className="rounded-xl border border-border bg-card p-5"
                >
                  <h3 className={`text-sm font-semibold ${item.color}`}>
                    {item.label}
                  </h3>
                  {latest && (
                    <div className="mt-3 space-y-2">
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">
                          Final Coverage
                        </span>
                        <span className="font-medium">
                          {latest.cumulativePercent.toFixed(2)}%
                        </span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">
                          Total Covered
                        </span>
                        <span className="font-medium">
                          {latest.coveredVerses.toLocaleString()}
                        </span>
                      </div>
                      {peakGrowth && peakGrowth.newVersesCount > 0 && (
                        <div className="flex justify-between text-sm">
                          <span className="text-muted-foreground">
                            Peak Growth
                          </span>
                          <span className="font-medium">
                            Century {toRoman(peakGrowth.century)} (+
                            {peakGrowth.newVersesCount.toLocaleString()})
                          </span>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {preset === "gospels-vs-epistles" && (
          <div className="rounded-xl border border-border bg-card p-6">
            <h2 className="mb-4 text-base font-semibold">
              Gospel Coverage Summary
            </h2>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    <th className="py-2 text-left font-medium text-muted-foreground">
                      Century
                    </th>
                    {["Matthew", "Mark", "Luke", "John"].map((name) => (
                      <th
                        key={name}
                        className="py-2 text-right font-medium text-muted-foreground"
                      >
                        {name}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {Array.from({ length: 10 }, (_, i) => i + 1).map(
                    (century) => (
                      <tr
                        key={century}
                        className="border-b border-border/50 hover:bg-secondary/30"
                      >
                        <td className="py-2 font-medium">
                          {toRoman(century)}
                        </td>
                        {[matthewData, markData, lukeData, johnData].map(
                          (d, i) => {
                            const entry = d?.entries.find(
                              (e) => e.century === century
                            );
                            return (
                              <td key={i} className="py-2 text-right">
                                {entry
                                  ? `${entry.cumulativePercent.toFixed(1)}%`
                                  : "-"}
                              </td>
                            );
                          }
                        )}
                      </tr>
                    )
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
