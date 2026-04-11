"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { useCreateApologeticTopic } from "@/hooks/useApologetics";
import { useAuth } from "@/hooks/useAuth";
import { useRouter } from "@/i18n/navigation";
import { Loader2, Sparkles, ArrowLeft } from "lucide-react";
import { Link } from "@/i18n/navigation";

function ProgressIndicator({ t }: { t: (key: string) => string }) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-primary/20 bg-primary/5 p-4">
      <Loader2 className="h-4 w-4 animate-spin text-primary" />
      <div>
        <p className="text-sm font-medium text-primary">{t("create.progress.title")}</p>
        <p className="mt-0.5 text-xs text-muted-foreground">{t("create.progress.queued")}</p>
      </div>
    </div>
  );
}

export default function NewApologeticTopicPage() {
  const t = useTranslations("apologetics");
  const tc = useTranslations("common");
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const createMutation = useCreateApologeticTopic();

  const [prompt, setPrompt] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim() || createMutation.isPending) return;
    const result = await createMutation.mutateAsync(prompt);
    router.push(`/apologetics/${result.slug}`);
  };

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen">
        <Header title={t("create.title")} />
        <div className="mx-auto w-full max-w-3xl p-4 md:p-6">
          <p className="text-sm text-muted-foreground">{tc("loginRequired")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <Header title={t("create.title")} subtitle={t("create.subtitle")} />

      <div className="mx-auto w-full max-w-3xl space-y-6 p-4 md:p-6">
        <Link
          href="/apologetics"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("backToList")}
        </Link>

        <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-border bg-card p-4 md:p-6">
          <div>
            <label className="mb-2 block text-sm font-medium">{t("create.promptLabel")}</label>
            <textarea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder={t("create.promptPlaceholder")}
              rows={8}
              className="w-full rounded-lg border border-border bg-background p-3 text-sm leading-relaxed focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              disabled={createMutation.isPending}
            />
          </div>

          {createMutation.isError && (
            <div className="rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-400">
              {(createMutation.error as Error).message}
            </div>
          )}

          <button
            type="submit"
            disabled={!prompt.trim() || createMutation.isPending}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
          >
            {createMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                {t("create.generating")}
              </>
            ) : (
              <>
                <Sparkles className="h-4 w-4" />
                {t("create.generate")}
              </>
            )}
          </button>

          {createMutation.isPending && <ProgressIndicator t={t} />}
        </form>
      </div>
    </div>
  );
}
