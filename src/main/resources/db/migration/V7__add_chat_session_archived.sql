ALTER TABLE chat_sessions ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_chat_sessions_archived ON chat_sessions(archived);
CREATE INDEX idx_chat_analytics_events_created_at ON chat_analytics_events(created_at);
