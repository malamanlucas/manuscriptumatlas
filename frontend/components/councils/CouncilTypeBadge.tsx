import type { CouncilType } from "@/types";

const TYPE_CLASS: Record<string, string> = {
  ECUMENICAL: "bg-[#b8976a]/15 text-[#b8976a]",
  REGIONAL: "bg-[#4a6fa5]/15 text-[#4a6fa5]",
  LOCAL: "bg-zinc-500/15 text-zinc-600 dark:text-zinc-400",
};

export function CouncilTypeBadge({ type }: { type: CouncilType | string }) {
  return (
    <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ${TYPE_CLASS[type] ?? TYPE_CLASS.LOCAL}`}>
      {type}
    </span>
  );
}
