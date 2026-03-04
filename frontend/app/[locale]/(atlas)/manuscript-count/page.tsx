"use client";

import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useManuscriptsCount } from "@/hooks/useStats";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import { ScrollText, FileText, BookOpen, PenTool } from "lucide-react";

const TYPE_COLORS: Record<string, string> = {
  Papyrus: "#f59e0b",
  Uncial: "#3b82f6",
  Minuscule: "#10b981",
  Lectionary: "#8b5cf6",
};

export default function ManuscriptCountPage() {
  const t = useTranslations("manuscriptCount");
  const tc = useTranslations("common");
  const { data, isLoading, error } = useManuscriptsCount();

  const chartData = data
    ? [
        { name: "Papyrus", count: data.papyrus },
        { name: "Uncial", count: data.uncial },
        { name: "Minuscule", count: data.minuscule },
        { name: "Lectionary", count: data.lectionary },
      ]
    : [];

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="mx-auto w-full max-w-5xl p-4 md:p-6 space-y-6">
        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            {t("loadingData")}
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            {tc("failedToLoad", { error: (error as Error).message })}
          </div>
        )}

        {data && (
          <>
            <div className="rounded-xl border border-border bg-card p-8 text-center">
              <p className="text-sm text-muted-foreground mb-2">
                {t("totalCatalogued")}
              </p>
              <p className="text-3xl font-bold md:text-5xl">{data.total.toLocaleString()}</p>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TypeCard
                label={t("papyri")}
                count={data.papyrus}
                icon={<ScrollText className="h-5 w-5 text-amber-500" />}
                color="bg-amber-100 dark:bg-amber-900"
              />
              <TypeCard
                label={t("uncials")}
                count={data.uncial}
                icon={<FileText className="h-5 w-5 text-blue-500" />}
                color="bg-blue-100 dark:bg-blue-900"
              />
              <TypeCard
                label={t("minuscules")}
                count={data.minuscule}
                icon={<PenTool className="h-5 w-5 text-emerald-500" />}
                color="bg-emerald-100 dark:bg-emerald-900"
              />
              <TypeCard
                label={t("lectionaries")}
                count={data.lectionary}
                icon={<BookOpen className="h-5 w-5 text-violet-500" />}
                color="bg-violet-100 dark:bg-violet-900"
              />
            </div>

            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="text-base font-semibold mb-4">
                {t("distributionByType")}
              </h3>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                    <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "var(--card)",
                        border: "1px solid var(--border)",
                        borderRadius: "8px",
                      }}
                    />
                    <Bar dataKey="count" name={tc("manuscripts")} radius={[4, 4, 0, 0]}>
                      {chartData.map((entry) => (
                        <Cell
                          key={entry.name}
                          fill={TYPE_COLORS[entry.name] ?? "#6b7280"}
                        />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="rounded-xl border border-border bg-card p-6 space-y-4">
              <h3 className="text-lg font-semibold">
                {t("gregoryAlandSystem")}
              </h3>
              <p className="text-muted-foreground leading-relaxed">
                {t("gregoryAlandDescription")}
              </p>
              <div className="space-y-3 text-muted-foreground leading-relaxed">
                <p>
                  <strong className="text-foreground">{t("papyriLabel")}</strong> — {t("papyriDescription")}
                </p>
                <p>
                  <strong className="text-foreground">{t("uncialsLabel")}</strong> — {t("uncialsDescription")}
                </p>
                <p>
                  <strong className="text-foreground">{t("minusculesLabel")}</strong> — {t("minusculesDescription")}
                </p>
                <p>
                  <strong className="text-foreground">{t("lectionariesLabel")}</strong> — {t("lectionariesDescription")}
                </p>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function TypeCard({
  label,
  count,
  icon,
  color,
}: {
  label: string;
  count: number;
  icon: React.ReactNode;
  color: string;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-center gap-3">
        <div className={`rounded-lg p-2 ${color}`}>{icon}</div>
        <div>
          <p className="text-2xl font-bold">{count.toLocaleString()}</p>
          <p className="text-xs text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  );
}
