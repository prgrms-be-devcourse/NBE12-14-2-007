UPDATE festival AS f
SET category = fs.category
    FROM festival_submission AS fs
WHERE f.id = fs.festival_id
  AND f.category IS NULL
  AND fs.category IS NOT NULL;

ALTER TABLE festival_submission
DROP COLUMN category;