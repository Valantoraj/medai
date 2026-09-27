-- V3: Create confidence scores table
CREATE TABLE IF NOT EXISTS confidence_scores (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT REFERENCES chat_sessions(id) ON DELETE CASCADE,
    condition_name  VARCHAR(100) NOT NULL,
    confidence_pct  DECIMAL(5,2) NOT NULL,
    reasoning       TEXT,
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(session_id, condition_name)
);

CREATE INDEX idx_confidence_session_id ON confidence_scores(session_id);
