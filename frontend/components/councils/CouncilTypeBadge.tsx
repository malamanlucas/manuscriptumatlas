import type { CouncilType } from "@/types";

const TYPE_CLASS: Record<string, string> = {
  ECUMENICAL: "bg-amber-500/15 text-amber-600 dark:text-amber-400",
  REGIONAL: "bg-blue-500/15 text-blue-600 dark:text-blue-400",
  LOCAL: "bg-zinc-500/15 text-zinc-600 dark:text-zinc-400",
};

export function CouncilTypeBadge({ type }: { type: CouncilType | string }) {
  return (
    <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ${TYPE_CLASS[type] ?? TYPE_CLASS.LOCAL}`}>
      {type}
    </span>
  );
}
