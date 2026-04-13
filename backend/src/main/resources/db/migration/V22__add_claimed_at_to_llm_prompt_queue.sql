-- Add claimed_at to track when processing began
-- Enables timeout-based reset of stale processing items
ALTER TABLE llm_prompt_queue ADD COLUMN claimed_at TIMESTAMPTZ;

CREATE INDEX idx_llm_queue_claimed_at ON llm_prompt_queue (claimed_at)
    WHERE status = 'processing';
