ALTER TABLE "like"
    ADD COLUMN festival_id BIGINT;

ALTER TABLE "like"
    ALTER COLUMN src_id DROP NOT NULL;

ALTER TABLE "like"
    ADD CONSTRAINT fk_like_festival
        FOREIGN KEY (festival_id)
            REFERENCES festival(id);

ALTER TABLE "like"
    ADD CONSTRAINT uk_like_festival_member
        UNIQUE (festival_id, member_id);