CREATE TABLE manager (
    id            UUID         NOT NULL,
    member_id     UUID         NOT NULL,
    organization   VARCHAR(255),
    company_phone  VARCHAR(255),
    reason        TEXT         NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    revoke_reason TEXT,
    created_at    TIMESTAMP(6) NOT NULL,
    updated_at    TIMESTAMP(6) NOT NULL,
    deleted_at  TIMESTAMP(6),
    CONSTRAINT pk_manager PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_manager_member ON manager (member_id);
