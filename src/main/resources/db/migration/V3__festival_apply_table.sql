CREATE TABLE festival_apply (
    id          UUID   NOT NULL,
    festival_id BIGINT NOT NULL,
    content     TEXT   NOT NULL,
    CONSTRAINT pk_festival_apply PRIMARY KEY (id)
);
