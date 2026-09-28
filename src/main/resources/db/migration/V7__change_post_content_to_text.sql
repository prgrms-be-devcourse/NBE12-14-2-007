ALTER TABLE post
ALTER COLUMN content TYPE TEXT
USING content::text;