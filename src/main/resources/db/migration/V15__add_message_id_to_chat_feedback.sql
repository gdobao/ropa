ALTER TABLE chat_feedback
    ADD COLUMN message_id UUID REFERENCES chat_messages(id) ON DELETE CASCADE;

CREATE INDEX idx_chat_feedback_message ON chat_feedback(message_id);
