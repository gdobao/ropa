CREATE TABLE anonymous_owners (
    id UUID PRIMARY KEY,
    bootstrap BOOLEAN NOT NULL DEFAULT FALSE,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO anonymous_owners (id, bootstrap, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', TRUE, NOW(), NOW());

ALTER TABLE garments ADD COLUMN owner_id UUID;
UPDATE garments SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;
ALTER TABLE garments ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE garments ADD CONSTRAINT fk_garments_owner FOREIGN KEY (owner_id) REFERENCES anonymous_owners(id);
CREATE INDEX idx_garments_owner_id ON garments(owner_id, created_at DESC);

ALTER TABLE week_plans ADD COLUMN owner_id UUID;
UPDATE week_plans wp
SET owner_id = g.owner_id
FROM garments g
WHERE wp.garment_id = g.id AND wp.owner_id IS NULL;
ALTER TABLE week_plans ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE week_plans ADD CONSTRAINT fk_week_plans_owner FOREIGN KEY (owner_id) REFERENCES anonymous_owners(id);
CREATE INDEX idx_week_plans_owner_day ON week_plans(owner_id, day_of_week, position);
