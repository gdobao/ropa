CREATE TABLE garments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(50) NOT NULL,
    color_name VARCHAR(50) NOT NULL,
    color_hex VARCHAR(7),
    material VARCHAR(80),
    season VARCHAR(50),
    image_url VARCHAR(500) NOT NULL,
    ai_type VARCHAR(50),
    ai_color_name VARCHAR(50),
    ai_color_hex VARCHAR(7),
    ai_confidence NUMERIC(3, 2),
    ai_model VARCHAR(80),
    user_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_garments_category ON garments(category);
CREATE INDEX idx_garments_color_name ON garments(color_name);
CREATE INDEX idx_garments_user_confirmed ON garments(user_confirmed);
CREATE INDEX idx_garments_created_at ON garments(created_at DESC);
