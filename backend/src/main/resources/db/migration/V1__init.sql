CREATE TABLE character_lookup (
    id            BIGSERIAL PRIMARY KEY,
    realm_slug    VARCHAR(100) NOT NULL,
    character_name VARCHAR(100) NOT NULL,
    looked_up_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_character_lookup_recent
    ON character_lookup (looked_up_at DESC);
