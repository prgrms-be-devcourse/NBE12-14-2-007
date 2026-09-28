ALTER TABLE festival
    ADD COLUMN region VARCHAR(32),
    ADD COLUMN region_detail VARCHAR(255);

CREATE INDEX idx_festival_region
    ON festival (region);