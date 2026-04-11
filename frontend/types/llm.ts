// ── LLM Usage ──

export interface LlmUsageLogDTO {
  id: number;
  provider: string;
  model: string;
  label: string;
  success: boolean;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  estimatedCostUsd: number;
  latencyMs: number;
  errorMessage?: string;
  createdAt: string;
}

export interface LlmProviderSummaryDTO {
  provider: string;
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  successRate: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalTokens: number;
  estimatedCostUsd: number;
  model?: string | null;
  avgLatencyMs?: number | null;
  errorBreakdown?: LlmErrorSummary[];
}

export interface RateLimiterStatusDTO {
  provider: string;
  remainingRequests?: number;
  remainingTokens?: number;
  resetTime?: string;
}

export interface LlmErrorSummary {
  errorType: string;
  count: number;
}

export interface LlmUsageDashboardDTO {
  providerSummaries: LlmProviderSummaryDTO[];
  modelSummaries: LlmProviderSummaryDTO[];
  errorSummary: LlmErrorSummary[];
  recentLogs: LlmUsageLogDTO[];
  rateLimiterStatus: RateLimiterStatusDTO[];
}

// ── LLM Queue ──

export interface QueuePhaseStatsDTO {
  phaseName: string;
  pending: number;
  processing: number;
  completed: number;
  applied: number;
  failed: number;
}

export interface QueueStatsDTO {
  totalPending: number;
  totalProcessing: number;
  totalCompleted: number;
  totalApplied: number;
  totalFailed: number;
  byPhase: QueuePhaseStatsDTO[];
}
