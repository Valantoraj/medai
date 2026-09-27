-- V5: Create user preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    known_conditions    TEXT[],
    medication_hist     TEXT[],
    allergies           TEXT[],
    language_style      VARCHAR(20) DEFAULT 'simple',
    updated_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_preferences_user_id ON user_preferences(user_id);
