-- V2: Create chat sessions and messages tables
CREATE TABLE IF NOT EXISTS chat_sessions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,
    bot_type    VARCHAR(30) NOT NULL,
    started_at  TIMESTAMP DEFAULT NOW(),
    ended_at    TIMESTAMP,
    is_active   BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_chat_sessions_bot_type ON chat_sessions(bot_type);
CREATE INDEX idx_chat_sessions_user_bot ON chat_sessions(user_id, bot_type);

CREATE TABLE IF NOT EXISTS chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(10) NOT NULL,
    content     TEXT NOT NULL,
    timestamp   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_timestamp ON chat_messages(timestamp);
