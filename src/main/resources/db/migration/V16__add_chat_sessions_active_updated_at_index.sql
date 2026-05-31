CREATE INDEX idx_chat_sessions_updated_at_active
    ON chat_sessions(updated_at)
    WHERE archived = false;
