WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY owner_id, day_of_week ORDER BY position, id) - 1 AS new_position
    FROM week_plans
)
UPDATE week_plans wp
SET position = ranked.new_position
FROM ranked
WHERE wp.id = ranked.id;

ALTER TABLE week_plans
    ADD CONSTRAINT uq_week_plans_owner_day_position
    UNIQUE (owner_id, day_of_week, position)
    DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX IF NOT EXISTS idx_week_plans_owner_garment ON week_plans(owner_id, garment_id);
CREATE INDEX IF NOT EXISTS idx_garments_owner_category_created ON garments(owner_id, category, created_at DESC);
