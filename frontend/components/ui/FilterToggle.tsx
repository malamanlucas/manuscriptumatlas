interface FilterToggleOption {
  value: string;
  label: string;
}

interface FilterToggleProps {
  options: FilterToggleOption[];
  value: string;
  onChange: (value: string) => void;
}

export function FilterToggle({ options, value, onChange }: FilterToggleProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((opt) => (
        <button
          key={opt.value}
          onClick={() => onChange(opt.value)}
          className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
            value === opt.value
              ? "bg-primary text-primary-foreground"
              : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
          }`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}
