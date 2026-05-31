ALTER TABLE chat_messages ADD COLUMN run_id UUID;

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_run
    FOREIGN KEY (run_id) REFERENCES chat_runs(id) ON DELETE SET NULL;

CREATE INDEX idx_chat_messages_run ON chat_messages(run_id);
