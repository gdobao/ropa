ALTER TABLE chat_sessions ADD COLUMN surface VARCHAR(50) NOT NULL DEFAULT 'MAIN_CHAT';

CREATE INDEX idx_chat_sessions_owner_surface_updated_at
    ON chat_sessions(owner_id, surface, updated_at DESC);
