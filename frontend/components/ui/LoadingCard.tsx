interface LoadingCardProps {
  rows?: number;
  height?: string;
}

export function LoadingCard({ rows = 1, height = "350px" }: LoadingCardProps) {
  return (
    <div className="rounded-xl border border-border bg-card p-4 md:p-6 animate-pulse">
      {rows > 1 ? (
        <div className="space-y-3">
          {Array.from({ length: rows }).map((_, i) => (
            <div
              key={i}
              className="h-4 rounded bg-muted"
              style={{ width: i === 0 ? "40%" : i % 3 === 0 ? "60%" : "80%" }}
            />
          ))}
        </div>
      ) : (
        <div className="rounded bg-muted" style={{ height }} role="status" aria-label="Loading" />
      )}
    </div>
  );
}
