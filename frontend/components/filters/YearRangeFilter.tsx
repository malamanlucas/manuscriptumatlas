"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useTranslations } from "next-intl";

const YEAR_MIN_BOUND = 30;
const YEAR_MAX_BOUND = 1100;
const DEBOUNCE_MS = 500;

interface YearRangeFilterProps {
  yearMin: number | undefined;
  yearMax: number | undefined;
  onChange: (min?: number, max?: number) => void;
  disabled?: boolean;
}

function clamp(value: number, lo: number, hi: number) {
  return Math.min(hi, Math.max(lo, value));
}

export function YearRangeFilter({
  yearMin,
  yearMax,
  onChange,
  disabled = false,
}: YearRangeFilterProps) {
  const t = useTranslations("yearFilter");

  const [localMin, setLocalMin] = useState(yearMin?.toString() ?? "");
  const [localMax, setLocalMax] = useState(yearMax?.toString() ?? "");
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setLocalMin(yearMin?.toString() ?? "");
  }, [yearMin]);

  useEffect(() => {
    setLocalMax(yearMax?.toString() ?? "");
  }, [yearMax]);

  const emitChange = useCallback(
    (rawMin: string, rawMax: string) => {
      if (timerRef.current) clearTimeout(timerRef.current);

      timerRef.current = setTimeout(() => {
        const parsedMin = rawMin === "" ? undefined : clamp(Number(rawMin), YEAR_MIN_BOUND, YEAR_MAX_BOUND);
        const parsedMax = rawMax === "" ? undefined : clamp(Number(rawMax), YEAR_MIN_BOUND, YEAR_MAX_BOUND);

        if (parsedMin !== undefined && parsedMax !== undefined && parsedMin > parsedMax) return;

        onChange(parsedMin, parsedMax);
      }, DEBOUNCE_MS);
    },
    [onChange],
  );

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  const handleMinChange = (raw: string) => {
    setLocalMin(raw);
    emitChange(raw, localMax);
  };

  const handleMaxChange = (raw: string) => {
    setLocalMax(raw);
    emitChange(localMin, raw);
  };

  const handleClear = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setLocalMin("");
    setLocalMax("");
    onChange(undefined, undefined);
  };

  const hasValue = localMin !== "" || localMax !== "";

  return (
    <div className="space-y-2">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:gap-4">
          <div className="flex shrink-0 flex-col gap-1 sm:min-w-[6rem]">
            <label className="shrink-0 text-xs text-muted-foreground whitespace-nowrap">{t("from")}</label>
            <input
              type="number"
              min={YEAR_MIN_BOUND}
              max={YEAR_MAX_BOUND}
              value={localMin}
              onChange={(e) => handleMinChange(e.target.value)}
              placeholder={t("placeholderFrom")}
              disabled={disabled}
              className="rounded-lg border border-input bg-background px-3 py-1.5 text-sm tabular-nums placeholder:text-muted-foreground/60 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>

          <div className="flex shrink-0 flex-col gap-1 sm:min-w-[6rem]">
            <label className="shrink-0 text-xs text-muted-foreground whitespace-nowrap">{t("to")}</label>
            <input
              type="number"
              min={YEAR_MIN_BOUND}
              max={YEAR_MAX_BOUND}
              value={localMax}
              onChange={(e) => handleMaxChange(e.target.value)}
              placeholder={t("placeholderTo")}
              disabled={disabled}
              className="rounded-lg border border-input bg-background px-3 py-1.5 text-sm tabular-nums placeholder:text-muted-foreground/60 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>
        </div>

        <div className="flex shrink-0 items-end gap-2">
          {hasValue && (
            <span className="inline-flex items-center rounded-lg bg-primary px-2.5 py-1.5 text-xs font-medium text-primary-foreground">
              {localMin || "…"}–{localMax || "…"}
            </span>
          )}

          <button
            type="button"
            onClick={handleClear}
            disabled={disabled || !hasValue}
            className="shrink-0 rounded-lg bg-secondary px-3 py-1.5 text-sm font-medium text-secondary-foreground transition-colors hover:bg-secondary/80 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {t("clear")}
          </button>
        </div>
      </div>
    </div>
  );
}
