-- Dedupe + enforce "at most 1 Attempt per Test" (task 6 spec, already stated in
-- StudentAttemptService's javadoc) at the DB level. Before this migration, attempt.test_id had NO
-- unique constraint, so a race between 2 concurrent StudentAttemptService#start calls for the same
-- testId (e.g. the Student opened the same Test in 2 browser tabs) could insert 2 Attempt rows -
-- both would look like "no attempt yet" to each other's transaction. #submit always updates the
-- correct row (looked up by attemptId), but StudentAttemptService#getOwnAttemptReport re-queries by
-- testId with no ORDER BY, which could then arbitrarily pick the OTHER, never-submitted duplicate -
-- producing a false "QUIZ_013 ATTEMPT_NOT_SUBMITTED" for a Student who genuinely did submit. See
-- StudentAttemptService#findAttemptForTest's javadoc for the matching application-layer fix.
--
-- Step 1: for every test_id with more than 1 Attempt row, keep exactly one - the SUBMITTED row if
-- one exists (that is always the row the Student actually finished with), else the most recently
-- created row - and delete the rest (their attempt_answer children first, to satisfy the FK).
DELETE FROM attempt_answer
WHERE attempt_id IN (
    SELECT id FROM (
        SELECT a.id AS id
        FROM attempt a
                 JOIN (SELECT test_id,
                              COALESCE(MAX(CASE WHEN submitted_at IS NOT NULL THEN id END), MAX(id)) AS keep_id
                       FROM attempt
                       GROUP BY test_id) k ON a.test_id = k.test_id
        WHERE a.id <> k.keep_id
    ) dup_ids
);

DELETE FROM attempt
WHERE id IN (
    SELECT id FROM (
        SELECT a.id AS id
        FROM attempt a
                 JOIN (SELECT test_id,
                              COALESCE(MAX(CASE WHEN submitted_at IS NOT NULL THEN id END), MAX(id)) AS keep_id
                       FROM attempt
                       GROUP BY test_id) k ON a.test_id = k.test_id
        WHERE a.id <> k.keep_id
    ) dup_ids
);

-- Step 2: now that every test_id maps to at most 1 row, enforce it going forward.
ALTER TABLE attempt
    ADD CONSTRAINT uq_attempt_test_id UNIQUE (test_id);
