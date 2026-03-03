-- ============================================================
-- V11: Visitor Tracking - partitioned tables + indices + helpers
-- ============================================================

-- 1. visitor_sessions (partitioned by month on created_at)
CREATE TABLE visitor_sessions (
    id                   BIGSERIAL,
    visitor_id           VARCHAR(36)   NOT NULL,
    session_id           VARCHAR(36)   NOT NULL,
    ip_address           VARCHAR(45)   NOT NULL,
    user_agent           TEXT          NOT NULL,
    browser_name         VARCHAR(50),
    browser_version      VARCHAR(30),
    os_name              VARCHAR(50),
    os_version           VARCHAR(30),
    device_type          VARCHAR(10),
    screen_width         SMALLINT,
    screen_height        SMALLINT,
    viewport_width       SMALLINT,
    viewport_height      SMALLINT,
    language             VARCHAR(10),
    languages            TEXT,
    timezone             VARCHAR(50),
    platform             VARCHAR(50),
    network_info         JSONB,
    device_memory        SMALLINT,
    hardware_concurrency SMALLINT,
    color_depth          SMALLINT,
    pixel_ratio          NUMERIC(4,2),
    touch_points         SMALLINT,
    cookie_enabled       BOOLEAN,
    do_not_track         BOOLEAN,
    webgl_renderer       VARCHAR(200),
    webgl_vendor         VARCHAR(200),
    canvas_fingerprint   VARCHAR(64),
    referrer             TEXT,
    page_load_time_ms    INTEGER,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_activity_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 2. page_views (partitioned by month on created_at)
CREATE TABLE page_views (
    id            BIGSERIAL,
    session_id    VARCHAR(36)   NOT NULL,
    visitor_id    VARCHAR(36)   NOT NULL,
    path          VARCHAR(500)  NOT NULL,
    referrer_path VARCHAR(500),
    duration_ms   INTEGER,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 3. visitor_daily_stats (small, not partitioned)
CREATE TABLE visitor_daily_stats (
    id                      SERIAL PRIMARY KEY,
    stat_date               DATE          NOT NULL UNIQUE,
    total_sessions          INTEGER       NOT NULL DEFAULT 0,
    total_pageviews         INTEGER       NOT NULL DEFAULT 0,
    unique_visitors         INTEGER       NOT NULL DEFAULT 0,
    avg_session_duration_ms INTEGER,
    top_browsers            JSONB,
    top_os                  JSONB,
    top_devices             JSONB,
    top_pages               JSONB,
    top_countries           JSONB,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Initial monthly partitions (current + 2 months ahead)
-- ============================================================
CREATE TABLE visitor_sessions_2026_03 PARTITION OF visitor_sessions
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE visitor_sessions_2026_04 PARTITION OF visitor_sessions
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE visitor_sessions_2026_05 PARTITION OF visitor_sessions
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE page_views_2026_03 PARTITION OF page_views
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE page_views_2026_04 PARTITION OF page_views
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE page_views_2026_05 PARTITION OF page_views
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

-- ============================================================
-- Indices on visitor_sessions
-- ============================================================
CREATE INDEX idx_vs_created_at       ON visitor_sessions (created_at DESC);
CREATE INDEX idx_vs_last_activity    ON visitor_sessions (last_activity_at DESC);
CREATE INDEX idx_vs_session_id       ON visitor_sessions (session_id);
CREATE INDEX idx_vs_visitor_id       ON visitor_sessions (visitor_id, created_at DESC);
CREATE INDEX idx_vs_ip               ON visitor_sessions (ip_address, created_at DESC);
CREATE INDEX idx_vs_fingerprint      ON visitor_sessions (canvas_fingerprint) WHERE canvas_fingerprint IS NOT NULL;
CREATE INDEX idx_vs_created_device   ON visitor_sessions (created_at DESC, device_type);
CREATE INDEX idx_vs_created_browser  ON visitor_sessions (created_at DESC, browser_name);
CREATE INDEX idx_vs_created_os       ON visitor_sessions (created_at DESC, os_name);
CREATE INDEX idx_vs_language         ON visitor_sessions (language, created_at DESC);
CREATE INDEX idx_vs_timezone         ON visitor_sessions (timezone, created_at DESC);

-- Indices on page_views
CREATE INDEX idx_pv_created_at       ON page_views (created_at DESC);
CREATE INDEX idx_pv_session_id       ON page_views (session_id, created_at ASC);
CREATE INDEX idx_pv_visitor_id       ON page_views (visitor_id, created_at DESC);
CREATE INDEX idx_pv_path_created     ON page_views (path, created_at DESC);

-- ============================================================
-- Helper: auto-create future monthly partitions
-- ============================================================
CREATE OR REPLACE FUNCTION create_monthly_partitions(months_ahead INT DEFAULT 2)
RETURNS void AS $$
DECLARE
    start_date DATE;
    end_date   DATE;
    part_name  TEXT;
    i          INT;
BEGIN
    FOR i IN 0..months_ahead LOOP
        start_date := date_trunc('month', CURRENT_DATE + (i || ' months')::INTERVAL);
        end_date   := start_date + INTERVAL '1 month';

        part_name := 'visitor_sessions_' || to_char(start_date, 'YYYY_MM');
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = part_name) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF visitor_sessions FOR VALUES FROM (%L) TO (%L)',
                part_name, start_date, end_date
            );
        END IF;

        part_name := 'page_views_' || to_char(start_date, 'YYYY_MM');
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = part_name) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF page_views FOR VALUES FROM (%L) TO (%L)',
                part_name, start_date, end_date
            );
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;
