-- V4: Create user interactions table (with pgvector for personalisation)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS user_interactions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id) ON DELETE CASCADE,
    feature         VARCHAR(30) NOT NULL,
    input_summary   TEXT,
    output_summary  TEXT,
    embedding       VECTOR(768),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_interactions_user_id ON user_interactions(user_id);
CREATE INDEX idx_interactions_feature ON user_interactions(feature);
-- Vector similarity search index (HNSW for fast ANN search)
CREATE INDEX idx_interactions_embedding ON user_interactions
    USING hnsw (embedding vector_cosine_ops);
