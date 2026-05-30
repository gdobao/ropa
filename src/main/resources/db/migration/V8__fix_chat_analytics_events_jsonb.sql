ALTER TABLE chat_analytics_events ALTER COLUMN event_data DROP DEFAULT;
ALTER TABLE chat_analytics_events ALTER COLUMN event_data SET DATA TYPE JSONB USING event_data::JSONB;
ALTER TABLE chat_analytics_events ALTER COLUMN event_data SET DEFAULT '{}'::JSONB;
