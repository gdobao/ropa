-- Deprecated seed migration.
--
-- This version intentionally does not write data. The original local prototype
-- migration inserted visual-test garments and reset sequences, which is unsafe
-- for real databases once Flyway is enabled by default.
--
-- To generate sample data in development, start the app and use the guarded
-- POST /wardrobe/seed flow, which only seeds the current anonymous owner when
-- that owner's wardrobe is empty.
SELECT 1;
