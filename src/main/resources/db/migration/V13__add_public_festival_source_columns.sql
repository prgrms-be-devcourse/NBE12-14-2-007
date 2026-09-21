-- 원본 스냅샷이 몇 건인지, 언제 저장/갱신됐는지 확인할 수 있도록 컬럼을 추가한다.
ALTER TABLE public_festival_source
    ADD COLUMN total_count INT NOT NULL,
    ADD COLUMN created_at TIMESTAMP(6) NOT NULL,
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL;
