-- V20: Apologetics module — skeptic arguments catalog with apologetic responses

CREATE TABLE apologetic_topics (
    id SERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(550) NOT NULL UNIQUE,
    original_prompt TEXT NOT NULL,
    body TEXT NOT NULL,
    body_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by_email VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE apologetic_responses (
    id SERIAL PRIMARY KEY,
    topic_id INT NOT NULL REFERENCES apologetic_topics(id) ON DELETE CASCADE,
    original_prompt TEXT NOT NULL,
    body TEXT NOT NULL,
    body_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    response_order INT NOT NULL DEFAULT 1,
    created_by_email VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE apologetic_topic_translations (
    id SERIAL PRIMARY KEY,
    topic_id INT NOT NULL REFERENCES apologetic_topics(id) ON DELETE CASCADE,
    locale VARCHAR(5) NOT NULL,
    title VARCHAR(500) NOT NULL,
    body TEXT,
    translation_source VARCHAR(20) NOT NULL DEFAULT 'ai',
    UNIQUE (topic_id, locale)
);

CREATE TABLE apologetic_response_translations (
    id SERIAL PRIMARY KEY,
    response_id INT NOT NULL REFERENCES apologetic_responses(id) ON DELETE CASCADE,
    locale VARCHAR(5) NOT NULL,
    body TEXT NOT NULL,
    translation_source VARCHAR(20) NOT NULL DEFAULT 'ai',
    UNIQUE (response_id, locale)
);

-- Indexes
CREATE INDEX idx_apologetic_topics_slug ON apologetic_topics (slug);
CREATE INDEX idx_apologetic_topics_status ON apologetic_topics (status);
CREATE INDEX idx_apologetic_topics_title_trgm ON apologetic_topics USING gin (title gin_trgm_ops);
CREATE INDEX idx_apologetic_topics_fts ON apologetic_topics USING gin (
    to_tsvector('english', coalesce(title,'') || ' ' || coalesce(body,''))
);
CREATE INDEX idx_apologetic_responses_topic ON apologetic_responses (topic_id);
CREATE INDEX idx_apologetic_topic_translations_lookup ON apologetic_topic_translations (locale, topic_id);
CREATE INDEX idx_apologetic_response_translations_lookup ON apologetic_response_translations (locale, response_id);
