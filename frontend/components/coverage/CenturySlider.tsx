"use client";

import { toRoman } from "@/lib/utils";

interface CenturySliderProps {
  value: number;
  onChange: (century: number) => void;
  min?: number;
  max?: number;
}

export function CenturySlider({
  value,
  onChange,
  min = 1,
  max = 10,
}: CenturySliderProps) {
  return (
    <div className="flex items-center gap-4">
      <label className="text-sm font-medium text-muted-foreground whitespace-nowrap">
        Seculo
      </label>
      <div className="flex items-center gap-3 flex-1">
        <span className="text-xs font-mono text-muted-foreground w-6 text-right">
          {toRoman(min)}
        </span>
        <input
          type="range"
          min={min}
          max={max}
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
          className="flex-1 h-2 rounded-lg appearance-none cursor-pointer accent-primary bg-secondary"
        />
        <span className="text-xs font-mono text-muted-foreground w-6">
          {toRoman(max)}
        </span>
      </div>
      <div className="flex items-center gap-2 rounded-lg bg-primary px-3 py-1.5">
        <span className="text-lg font-bold text-primary-foreground">
          {toRoman(value)}
        </span>
      </div>
    </div>
  );
}
