"use client";

import { useState, useCallback } from "react";
import { toRoman } from "@/lib/utils";
import { useTranslations } from "next-intl";

interface CenturyRangeFilterProps {
  value: number | undefined;
  onChange: (century: number | undefined) => void;
  min?: number;
  max?: number;
}

export function CenturyRangeFilter({
  value,
  onChange,
  min = 1,
  max = 10,
}: CenturyRangeFilterProps) {
  const t = useTranslations("centuryFilter");
  const tc = useTranslations("common");

  const [sliderPosition, setSliderPosition] = useState(value ?? Math.ceil((min + max) / 2));

  const isAllActive = value === undefined;

  const handleSliderChange = useCallback(
    (newValue: number) => {
      setSliderPosition(newValue);
      onChange(newValue);
    },
    [onChange],
  );

  const handleTickClick = useCallback(
    (century: number) => {
      setSliderPosition(century);
      onChange(century);
    },
    [onChange],
  );

  const handleAllClick = useCallback(() => {
    onChange(undefined);
  }, [onChange]);

  const centuries = Array.from({ length: max - min + 1 }, (_, i) => min + i);

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-3">
        <button
          onClick={handleAllClick}
          className={`shrink-0 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
            isAllActive
              ? "bg-primary text-primary-foreground"
              : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
          }`}
        >
          {t("all")}
        </button>

        <div className="relative flex-1 px-1">
          <input
            type="range"
            min={min}
            max={max}
            value={sliderPosition}
            onChange={(e) => handleSliderChange(Number(e.target.value))}
            className={`w-full h-2 rounded-lg appearance-none cursor-pointer accent-primary bg-secondary transition-opacity ${
              isAllActive ? "opacity-40" : ""
            }`}
            aria-label={t("sliderLabel")}
          />

          <div className="flex justify-between mt-1.5" style={{ padding: "0 2px" }}>
            {centuries.map((c) => (
              <button
                key={c}
                onClick={() => handleTickClick(c)}
                className={`text-[10px] font-mono transition-colors px-0.5 ${
                  !isAllActive && sliderPosition === c
                    ? "text-primary font-bold"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {toRoman(c)}
              </button>
            ))}
          </div>
        </div>

        <div
          className={`shrink-0 flex items-center gap-1.5 rounded-lg px-3 py-1.5 transition-colors ${
            isAllActive
              ? "bg-secondary text-secondary-foreground"
              : "bg-primary text-primary-foreground"
          }`}
        >
          <span className="text-sm font-bold whitespace-nowrap">
            {isAllActive
              ? t("allCenturies")
              : `${tc("century")} ${toRoman(sliderPosition)}`}
          </span>
        </div>
      </div>
    </div>
  );
}
