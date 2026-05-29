CREATE TABLE week_plans (
    id BIGSERIAL PRIMARY KEY,
    garment_id BIGINT NOT NULL REFERENCES garments(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_week_plans_day ON week_plans(day_of_week, position);
CREATE INDEX idx_week_plans_garment ON week_plans(garment_id);
