CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES anonymous_owners(id),
    title VARCHAR(255) NOT NULL DEFAULT 'Nueva conversación',
    model VARCHAR(100) NOT NULL DEFAULT 'gpt-4o',
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES anonymous_owners(id),
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    tokens INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_runs (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES anonymous_owners(id),
    model_requested VARCHAR(100) NOT NULL,
    model_resolved VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    total_tokens INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_feedback (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES chat_runs(id) ON DELETE CASCADE,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES anonymous_owners(id),
    rating VARCHAR(50) NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_analytics_events (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES anonymous_owners(id),
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_sessions_owner ON chat_sessions(owner_id, updated_at DESC);
CREATE INDEX idx_chat_messages_session ON chat_messages(session_id, created_at ASC);
CREATE INDEX idx_chat_messages_owner ON chat_messages(owner_id, created_at DESC);
CREATE INDEX idx_chat_runs_session ON chat_runs(session_id, started_at DESC);
CREATE INDEX idx_chat_runs_owner ON chat_runs(owner_id, started_at DESC);
CREATE INDEX idx_chat_feedback_run ON chat_feedback(run_id);
CREATE INDEX idx_chat_feedback_session ON chat_feedback(session_id);
CREATE INDEX idx_chat_analytics_events_type ON chat_analytics_events(event_type, created_at DESC);
CREATE INDEX idx_chat_analytics_events_owner ON chat_analytics_events(owner_id, created_at DESC);