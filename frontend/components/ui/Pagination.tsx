"use client";

import { useTranslations } from "next-intl";

interface PaginationProps {
  page: number;
  total: number;
  limit: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, total, limit, onChange }: PaginationProps) {
  const tc = useTranslations("common");
  const totalPages = Math.ceil(total / limit);

  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-center gap-3 pt-2 text-sm text-muted-foreground">
      <button
        onClick={() => onChange(page - 1)}
        disabled={page <= 1}
        className="rounded-md px-3 py-1.5 hover:bg-accent hover:text-foreground disabled:pointer-events-none disabled:opacity-40"
      >
        ← {tc("previous")}
      </button>
      <span>
        {page} / {totalPages}
      </span>
      <button
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages}
        className="rounded-md px-3 py-1.5 hover:bg-accent hover:text-foreground disabled:pointer-events-none disabled:opacity-40"
      >
        {tc("next")} →
      </button>
    </div>
  );
}
