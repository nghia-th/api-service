package vn.org.thn.service.app.quiz.entity;

/**
 * The 2 values {@link Test#getTestType()} can hold - stored as its {@code name()} in the plain
 * {@code test_type} column, same "enum, but the entity field itself stays a plain String" pattern
 * as {@link TestStatus}/{@code Test#getStatus()} (see that class's javadoc for the reasoning).
 * <p>
 * REGULAR - a Test created by a Parent via {@code POST /api/parent/tests} with an explicit
 * {@code questionIds} list (task 5). Counted in the Parent Dashboard's "Đề đã giao"/"Đề đã hoàn
 * thành" stats and in every existing report/listing exactly as before this field was added -
 * every pre-existing row was backfilled to REGULAR by {@code V8__test_type.sql}.
 * <p>
 * PRACTICE - a self-review "Ôn tập kiến thức" Test: N random Questions picked from every Lesson
 * under one Subject (see {@code TestService#generatePractice}/{@code #generatePracticeForStudent}),
 * requested added 2026-09-01 ("em xem cần làm thêm chức năng ôn tập kiến thức giống như ra một đề
 * thi của từng môn vậy"). Either the Parent or the Student themselves can generate one, and every
 * regenerate/retake creates a brand-new Test row rather than reusing an existing one - this is
 * deliberate, so v1's "at most 1 Attempt per Test" rule ({@code StudentAttemptService#start}'s
 * idempotent-start behavior) never has to change: "retake" means "a new Test", never "a new
 * Attempt on the same Test". Deliberately EXCLUDED from the Parent Dashboard's regular counts and
 * shown separately (tagged) in Reports, per the user's explicit answer when this feature was
 * scoped - never dilutes the REGULAR-only stats.
 */
public enum TestType {
    REGULAR,
    PRACTICE
}
