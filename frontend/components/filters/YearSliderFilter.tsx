"use client";

import { useState, useCallback } from "react";
import { useTranslations } from "next-intl";

const YEAR_MIN = 0;
const YEAR_MAX = 1000;
const STEP = 10;
const TICK_INTERVAL = 100;

interface YearSliderFilterProps {
  yearTo: number | undefined;
  onYearChange: (to: number | undefined) => void;
}

export function YearSliderFilter({
  yearTo,
  onYearChange,
}: YearSliderFilterProps) {
  const t = useTranslations("yearSlider");

  const [sliderPosition, setSliderPosition] = useState(yearTo ?? 0);
  const isActive = yearTo !== undefined;

  const handleSliderChange = useCallback(
    (value: number) => {
      setSliderPosition(value);
      onYearChange(value);
    },
    [onYearChange],
  );

  const handleTickClick = useCallback(
    (year: number) => {
      setSliderPosition(year);
      onYearChange(year);
    },
    [onYearChange],
  );

  const handleClear = useCallback(() => {
    onYearChange(undefined);
  }, [onYearChange]);

  const ticks = Array.from(
    { length: Math.floor((YEAR_MAX - YEAR_MIN) / TICK_INTERVAL) + 1 },
    (_, i) => YEAR_MIN + i * TICK_INTERVAL,
  );

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        {/* Slider */}
        <div className="relative flex-1 px-1">
          <input
            type="range"
            min={YEAR_MIN}
            max={YEAR_MAX}
            step={STEP}
            value={sliderPosition}
            onChange={(e) => handleSliderChange(Number(e.target.value))}
            className={`h-2 w-full cursor-pointer appearance-none rounded-lg bg-secondary accent-primary transition-opacity ${
              !isActive ? "opacity-40" : ""
            }`}
            aria-label={t("rangeLabel")}
          />

          <div className="mt-1.5 flex justify-between" style={{ padding: "0 2px" }}>
            {ticks.map((year) => (
              <button
                key={year}
                onClick={() => handleTickClick(year)}
                className={`px-0.5 font-mono text-[10px] transition-colors ${
                  year !== YEAR_MIN && year !== YEAR_MAX && year !== 500
                    ? "hidden sm:inline"
                    : ""
                } ${
                  isActive && year <= sliderPosition
                    ? "font-bold text-primary"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {year}
              </button>
            ))}
          </div>
        </div>

        {/* Badge + Clear */}
        <div className="flex shrink-0 items-center gap-2">
          {isActive && (
            <span className="inline-flex items-center rounded-lg bg-primary px-2.5 py-1.5 text-xs font-medium text-primary-foreground">
              0–{sliderPosition} AD
            </span>
          )}

          <button
            type="button"
            onClick={handleClear}
            disabled={!isActive}
            className="shrink-0 rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium text-secondary-foreground transition-colors hover:bg-secondary/80 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {t("clear")}
          </button>
        </div>
      </div>

    </div>
  );
}
