DROP INDEX IF EXISTS idx_chat_sessions_owner_surface_updated_at;

CREATE INDEX idx_chat_sessions_owner_surface_active_updated_at
    ON chat_sessions(owner_id, surface, updated_at DESC)
    WHERE archived = false;
