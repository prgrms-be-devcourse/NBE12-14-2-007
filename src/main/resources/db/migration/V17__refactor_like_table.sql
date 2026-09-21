ALTER TABLE "like"
DROP COLUMN "like";

ALTER TABLE "like"
    ADD CONSTRAINT uk_like_post_member
        UNIQUE (src_id, member_id);