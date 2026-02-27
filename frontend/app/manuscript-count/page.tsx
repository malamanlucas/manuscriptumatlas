"use client";

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
        title="Quantos Manuscritos Existem?"
        subtitle="Total de manuscritos gregos do Novo Testamento"
      />

      <div className="p-6 space-y-6 max-w-5xl">
        {isLoading && (
          <div className="rounded-xl border border-border bg-card p-8 text-center text-muted-foreground">
            Carregando dados...
          </div>
        )}

        {error && (
          <div className="rounded-xl border border-red-300 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
            Falha ao carregar: {(error as Error).message}
          </div>
        )}

        {data && (
          <>
            <div className="rounded-xl border border-border bg-card p-8 text-center">
              <p className="text-sm text-muted-foreground mb-2">
                Total de Manuscritos Catalogados
              </p>
              <p className="text-5xl font-bold">{data.total.toLocaleString()}</p>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <TypeCard
                label="Papiros"
                count={data.papyrus}
                icon={<ScrollText className="h-5 w-5 text-amber-500" />}
                color="bg-amber-100 dark:bg-amber-900"
              />
              <TypeCard
                label="Unciais"
                count={data.uncial}
                icon={<FileText className="h-5 w-5 text-blue-500" />}
                color="bg-blue-100 dark:bg-blue-900"
              />
              <TypeCard
                label="Minúsculos"
                count={data.minuscule}
                icon={<PenTool className="h-5 w-5 text-emerald-500" />}
                color="bg-emerald-100 dark:bg-emerald-900"
              />
              <TypeCard
                label="Lecionários"
                count={data.lectionary}
                icon={<BookOpen className="h-5 w-5 text-violet-500" />}
                color="bg-violet-100 dark:bg-violet-900"
              />
            </div>

            <div className="rounded-xl border border-border bg-card p-6">
              <h3 className="text-base font-semibold mb-4">
                Distribuição por Tipo
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
                    <Bar dataKey="count" name="Manuscritos" radius={[4, 4, 0, 0]}>
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
                O Sistema Gregory-Aland
              </h3>
              <p className="text-muted-foreground leading-relaxed">
                O sistema de catalogação Gregory-Aland é o padrão internacional para
                identificar manuscritos gregos do Novo Testamento. Criado por Caspar
                René Gregory e atualizado por Kurt Aland, classifica os manuscritos
                em quatro categorias:
              </p>
              <div className="space-y-3 text-muted-foreground leading-relaxed">
                <p>
                  <strong className="text-foreground">Papiros (P)</strong> — Escritos em papiro,
                  são os testemunhos mais antigos. Designados com o prefixo P seguido de
                  número (ex.: P52, P66, P75). A maioria vem do Egito, datando dos
                  séculos II–VII.
                </p>
                <p>
                  <strong className="text-foreground">Unciais (0)</strong> — Manuscritos em pergaminho
                  escritos em letras maiúsculas (unciais). Incluem os grandes códices como
                  Sinaiticus (01), Vaticanus (03) e Alexandrinus (02). Datam dos séculos
                  III–X.
                </p>
                <p>
                  <strong className="text-foreground">Minúsculos</strong> — Manuscritos em escrita
                  cursiva minúscula, predominante a partir do século IX. Representam a
                  maioria dos manuscritos do NT, numerados de 1 a 2900+.
                </p>
                <p>
                  <strong className="text-foreground">Lecionários (l)</strong> — Manuscritos litúrgicos
                  que contêm trechos do NT organizados para leitura pública no culto.
                  Designados com o prefixo l (ex.: l1, l2).
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
