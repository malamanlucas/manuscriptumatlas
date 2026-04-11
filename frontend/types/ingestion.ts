export interface IngestionStatusResponse {
  status: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
  manuscriptsIngested: number;
  versesLinked: number;
  errorMessage: string | null;
  isRunning: boolean;
  enableIngestion: boolean;
}

export interface PhaseStatusDTO {
  phaseName: string;
  status: "idle" | "running" | "success" | "failed" | string;
  startedAt?: string | null;
  completedAt?: string | null;
  itemsProcessed: number;
  itemsTotal: number;
  errorMessage?: string | null;
  lastRunBy?: string | null;
}

export interface CacheEntryDTO {
  key: string;
  sizeBytes: number;
}

export interface CacheStatsDTO {
  totalFiles: number;
  totalSizeBytes: number;
  totalSizeMb: number;
  entries: CacheEntryDTO[];
}
