-- Tabela para participantes hereticos em concilios (texto apenas; futura ingestao de "pais hereges")
CREATE TABLE IF NOT EXISTS council_heretic_participants (
    id SERIAL PRIMARY KEY,
    council_id INTEGER NOT NULL REFERENCES councils(id) ON DELETE CASCADE,
    display_name VARCHAR(300) NOT NULL,
    normalized_name VARCHAR(300) NOT NULL,
    role VARCHAR(100),
    description TEXT,
    UNIQUE (council_id, normalized_name)
);

CREATE INDEX IF NOT EXISTS idx_council_heretic_participants_council ON council_heretic_participants(council_id);
