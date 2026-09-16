ALTER TABLE festival_apply
    RENAME TO festival_submission;

ALTER TABLE festival_submission
    RENAME CONSTRAINT pk_festival_apply TO pk_festival_submission;

ALTER TABLE festival_submission
    ADD COLUMN category VARCHAR(50) NOT NULL;
