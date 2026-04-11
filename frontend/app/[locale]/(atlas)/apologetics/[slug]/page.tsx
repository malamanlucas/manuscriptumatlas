"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import {
  useApologeticTopicDetail,
  useUpdateApologeticTopic,
  useCreateApologeticResponse,
  useDeleteApologeticResponse,
} from "@/hooks/useApologetics";
import { useAuth } from "@/hooks/useAuth";
import { Link } from "@/i18n/navigation";
import {
  ArrowLeft,
  Pencil,
  Save,
  X,
  Loader2,
  Sparkles,
  Trash2,
  MessageSquare,
  FileText,
} from "lucide-react";
import Markdown from "react-markdown";

export default function ApologeticTopicDetailPage() {
  const params = useParams();
  const slug = params.slug as string;
  const t = useTranslations("apologetics");
  const tc = useTranslations("common");
  const { isAdmin } = useAuth();

  const { data: topic, isLoading, error } = useApologeticTopicDetail(slug);
  const updateMutation = useUpdateApologeticTopic();
  const deleteMutation = useDeleteApologeticResponse();
  const createResponseMutation = useCreateApologeticResponse(topic?.id ?? 0);

  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editBody, setEditBody] = useState("");

  const [responseText, setResponseText] = useState("");
  const [useAi, setUseAi] = useState(false);

  const startEditing = () => {
    if (!topic) return;
    setEditTitle(topic.title);
    setEditBody(topic.body);
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!topic) return;
    await updateMutation.mutateAsync({ id: topic.id, data: { title: editTitle, body: editBody } });
    setEditing(false);
  };

  const submitResponse = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!responseText.trim() || createResponseMutation.isPending) return;
    await createResponseMutation.mutateAsync({ body: responseText, useAi });
    setResponseText("");
    setUseAi(false);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen">
        <Header title={t("title")} />
        <div className="mx-auto w-full max-w-4xl p-4 md:p-6">
          <div className="h-8 w-64 animate-pulse rounded bg-secondary" />
          <div className="mt-4 h-48 animate-pulse rounded bg-secondary" />
        </div>
      </div>
    );
  }

  if (error || !topic) {
    return (
      <div className="min-h-screen">
        <Header title={t("title")} />
        <div className="mx-auto w-full max-w-4xl p-4 md:p-6">
          <p className="text-sm text-red-500">{error ? (error as Error).message : "Topic not found"}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <Header title={t("detailTitle")} />

      <div className="mx-auto w-full max-w-4xl space-y-6 p-4 md:p-6">
        <Link
          href="/apologetics"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("backToList")}
        </Link>

        {/* Topic card */}
        <div className="rounded-xl border border-border bg-card p-4 md:p-6">
          <div className="mb-3 flex items-center gap-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">
            <FileText className="h-4 w-4" />
            {t("skepticArgument")}
          </div>

          {topic.status === "PROCESSING" ? (
            <div className="space-y-4">
              <div className="flex items-center gap-3 rounded-lg border border-primary/20 bg-primary/5 p-4">
                <Loader2 className="h-5 w-5 animate-spin text-primary" />
                <div>
                  <p className="text-sm font-medium">{t("processingBanner")}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{t("processingHint")}</p>
                </div>
              </div>
              <div>
                <p className="mb-1 text-xs font-medium text-muted-foreground">{t("originalPrompt")}</p>
                <div className="whitespace-pre-wrap rounded-lg border border-border bg-background p-3 text-sm leading-relaxed text-foreground/80">
                  {topic.originalPrompt}
                </div>
              </div>
            </div>
          ) : editing ? (
            <div className="space-y-3">
              <input
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-lg font-semibold focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              />
              <textarea
                value={editBody}
                onChange={(e) => setEditBody(e.target.value)}
                rows={10}
                className="w-full rounded-lg border border-border bg-background p-3 text-sm leading-relaxed focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              />
              <div className="flex gap-2">
                <button
                  onClick={saveEdit}
                  disabled={updateMutation.isPending}
                  className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                >
                  <Save className="h-3.5 w-3.5" />
                  {t("save")}
                </button>
                <button
                  onClick={() => setEditing(false)}
                  className="inline-flex items-center gap-1 rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium"
                >
                  <X className="h-3.5 w-3.5" />
                  {t("cancel")}
                </button>
              </div>
            </div>
          ) : (
            <>
              <div className="flex items-start justify-between gap-3">
                <h1 className="text-xl font-semibold">{topic.title}</h1>
                {isAdmin && (
                  <button
                    onClick={startEditing}
                    className="inline-flex shrink-0 items-center gap-1 rounded-lg bg-secondary px-2.5 py-1.5 text-xs font-medium hover:bg-secondary/80"
                  >
                    <Pencil className="h-3 w-3" />
                    {t("editTopic")}
                  </button>
                )}
              </div>
              <div className="prose prose-sm dark:prose-invert mt-4 max-w-none text-foreground/90">
                <Markdown>{topic.body}</Markdown>
              </div>
              <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                <span>{new Date(topic.createdAt).toLocaleDateString()}</span>
                {topic.bodyReviewed && (
                  <>
                    <span>·</span>
                    <span className="text-green-600 dark:text-green-400">{t("reviewed")}</span>
                  </>
                )}
              </div>
            </>
          )}
        </div>

        {/* Responses section */}
        <div className="space-y-4">
          <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">
            <MessageSquare className="h-4 w-4" />
            {t("apologeticResponse")} ({topic.responses.length})
          </div>

          {topic.responses.map((resp) => (
            <div key={resp.id} className="rounded-xl border border-border bg-card p-4 md:p-6">
              <div className="prose prose-sm dark:prose-invert max-w-none text-foreground/90">
                <Markdown>{resp.body}</Markdown>
              </div>
              <div className="mt-3 flex items-center justify-between">
                <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                  <span>#{resp.responseOrder}</span>
                  <span>·</span>
                  <span>{new Date(resp.createdAt).toLocaleDateString()}</span>
                  {resp.bodyReviewed && (
                    <>
                      <span>·</span>
                      <span className="text-green-600 dark:text-green-400">{t("reviewed")}</span>
                    </>
                  )}
                </div>
                {isAdmin && (
                  <button
                    onClick={() => deleteMutation.mutate(resp.id)}
                    className="text-xs text-red-500 hover:text-red-700"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                )}
              </div>
            </div>
          ))}

          {/* Add response form */}
          {isAdmin && topic.status !== "PROCESSING" && (
            <form onSubmit={submitResponse} className="rounded-xl border border-border bg-card p-4 md:p-6">
              <label className="mb-2 block text-sm font-medium">{t("addResponse")}</label>
              <textarea
                value={responseText}
                onChange={(e) => setResponseText(e.target.value)}
                placeholder={t("create.responsePromptPlaceholder")}
                rows={6}
                className="w-full rounded-lg border border-border bg-background p-3 text-sm leading-relaxed focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                disabled={createResponseMutation.isPending}
              />

              <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <label className="inline-flex cursor-pointer items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={useAi}
                    onChange={(e) => setUseAi(e.target.checked)}
                    className="h-4 w-4 rounded border-border"
                    disabled={createResponseMutation.isPending}
                  />
                  <Sparkles className="h-3.5 w-3.5 text-primary" />
                  {t("create.complementWithAi")}
                </label>

                <button
                  type="submit"
                  disabled={!responseText.trim() || createResponseMutation.isPending}
                  className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                >
                  {createResponseMutation.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      {tc("loading")}
                    </>
                  ) : (
                    t("create.saveResponse")
                  )}
                </button>
              </div>

              {createResponseMutation.isPending && useAi && (
                <div className="mt-3 flex items-center gap-2 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-primary">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  {t("create.progress.complementing")}
                </div>
              )}

              {createResponseMutation.isError && (
                <div className="mt-3 rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-400">
                  {(createResponseMutation.error as Error).message}
                </div>
              )}
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
