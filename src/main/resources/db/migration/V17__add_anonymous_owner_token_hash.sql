ALTER TABLE anonymous_owners ADD COLUMN token_hash VARCHAR(64);
CREATE UNIQUE INDEX idx_anonymous_owners_token_hash ON anonymous_owners(token_hash) WHERE token_hash IS NOT NULL;
