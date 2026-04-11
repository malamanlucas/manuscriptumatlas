CREATE TABLE llm_prompt_queue (
    id              SERIAL PRIMARY KEY,
    phase_name      VARCHAR(100) NOT NULL,
    label           VARCHAR(200) NOT NULL,
    system_prompt   TEXT NOT NULL,
    user_content    TEXT NOT NULL,
    temperature     DOUBLE PRECISION NOT NULL DEFAULT 0.3,
    max_tokens      INTEGER NOT NULL DEFAULT 1024,
    tier            VARCHAR(10) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    response_content TEXT,
    model_used      VARCHAR(50),
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    error_message   TEXT,
    callback_context TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    batch_id        VARCHAR(100)
);

CREATE INDEX idx_llm_queue_status ON llm_prompt_queue(status);
CREATE INDEX idx_llm_queue_phase ON llm_prompt_queue(phase_name);
CREATE INDEX idx_llm_queue_batch ON llm_prompt_queue(batch_id) WHERE batch_id IS NOT NULL;
CREATE INDEX idx_llm_queue_status_tier ON llm_prompt_queue(status, tier);
CREATE INDEX idx_llm_queue_created ON llm_prompt_queue(created_at DESC);
