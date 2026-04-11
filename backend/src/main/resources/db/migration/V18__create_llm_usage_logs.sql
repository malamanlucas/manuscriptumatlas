CREATE TABLE llm_usage_logs (
    id SERIAL PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    model VARCHAR(50) NOT NULL,
    label VARCHAR(200) NOT NULL DEFAULT '',
    success BOOLEAN NOT NULL,
    input_tokens INT NOT NULL DEFAULT 0,
    output_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    estimated_cost_usd NUMERIC(10,6) NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_llm_usage_logs_created_at ON llm_usage_logs(created_at DESC);
CREATE INDEX idx_llm_usage_logs_provider ON llm_usage_logs(provider);
