-- Bugfix (2026-09-05): answer_mode was added as NOT NULL DEFAULT 'AUDIO' in V11, but
-- QuestionService#normalizeAnswerMode deliberately returns NULL for MULTIPLE_CHOICE questions
-- (answer_mode is only meaningful for SPEAKING - see Question.java's javadoc). The application
-- always sends an explicit value for every column (MyBatis-generated INSERT lists every column),
-- so the column DEFAULT never kicks in for an explicit NULL - every attempt to create a
-- MULTIPLE_CHOICE question failed with a NOT NULL constraint violation. Fix: drop the NOT NULL
-- constraint (existing rows keep their current value, nothing is backfilled).
ALTER TABLE question MODIFY COLUMN answer_mode VARCHAR(20) NULL DEFAULT 'AUDIO';
