"use client";

import { useState } from "react";
import { Link } from "@/i18n/navigation";
import { useTranslations } from "next-intl";
import { Header } from "@/components/layout/Header";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface FaqItem {
  question: string;
  answer: React.ReactNode;
}

interface FaqCategory {
  title: string;
  items: FaqItem[];
}

function buildFaqData(t: ReturnType<typeof useTranslations<"faq">>): FaqCategory[] {
  return [
    {
      title: t("skepticQuestions"),
      items: [
        {
          question: t("q1"),
          answer: t("a1"),
        },
        {
          question: t("q2"),
          answer: (
            <>
              {t("a2_part1")}
              <Link href="/timeline" className="text-primary hover:underline">
                {t("a2_link")}
              </Link>
              {t("a2_part2")}
            </>
          ),
        },
        {
          question: t("q3"),
          answer: t("a3"),
        },
        {
          question: t("q4"),
          answer: t("a4"),
        },
        {
          question: t("q5"),
          answer: (
            <>
              {t("a5_part1")}
              <Link href="/manuscripts" className="text-primary hover:underline">
                {t("a5_link")}
              </Link>
              {t("a5_part2")}
            </>
          ),
        },
      ],
    },
    {
      title: t("apologistQuestions"),
      items: [
        {
          question: t("q6"),
          answer: t("a6"),
        },
        {
          question: t("q7"),
          answer: t("a7"),
        },
        {
          question: t("q8"),
          answer: (
            <>
              {t("a8_part1")}
              <Link href="/metrics" className="text-primary hover:underline">
                {t("a8_link")}
              </Link>
              {t("a8_part2")}
            </>
          ),
        },
        {
          question: t("q9"),
          answer: (
            <>
              {t("a9_part1")}
              <Link href="/dashboard" className="text-primary hover:underline">
                {t("a9_link")}
              </Link>
              {t("a9_part2")}
            </>
          ),
        },
      ],
    },
    {
      title: t("technicalQuestions"),
      items: [
        {
          question: t("q10"),
          answer: (
            <>
              {t("a10_part1", { formula: "1 - (1 / avg_manuscripts_per_verse)" })}
              <Link href="/metrics" className="text-primary hover:underline">
                {t("a10_link")}
              </Link>
              {t("a10_part2")}
            </>
          ),
        },
        {
          question: t("q11"),
          answer: t("a11", {
            formula: "(coverage_N - coverage_N-1) / coverage_N-1 * 100",
          }),
        },
        {
          question: t("q12"),
          answer: t("a12", {
            formula: "covered_verses / manuscript_count",
          }),
        },
        {
          question: t("q13"),
          answer: (
            <>
              {t("a13_part1")}
              <Link href="/timeline" className="text-primary hover:underline">
                {t("a13_link")}
              </Link>
              {t("a13_part2")}
            </>
          ),
        },
      ],
    },
  ];
}

export default function FaqPage() {
  const t = useTranslations("faq");
  const faqData = buildFaqData(t);

  return (
    <div className="min-h-screen">
      <Header
        title={t("title")}
        subtitle={t("subtitle")}
      />

      <div className="p-4 md:p-6 space-y-8 max-w-3xl">
        {faqData.map((category) => (
          <section key={category.title}>
            <h2 className="text-base font-semibold mb-3">{category.title}</h2>
            <div className="space-y-2">
              {category.items.map((item) => (
                <AccordionItem key={item.question} item={item} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}

function AccordionItem({ item }: { item: FaqItem }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="rounded-xl border border-border bg-card overflow-hidden">
      <button
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-5 py-4 text-left text-sm font-medium hover:bg-muted/30 transition-colors"
      >
        <span>{item.question}</span>
        <ChevronDown
          className={cn(
            "h-4 w-4 shrink-0 text-muted-foreground transition-transform",
            open && "rotate-180"
          )}
        />
      </button>
      {open && (
        <div className="border-t border-border px-5 py-4 text-sm text-muted-foreground leading-relaxed">
          {item.answer}
        </div>
      )}
    </div>
  );
}
