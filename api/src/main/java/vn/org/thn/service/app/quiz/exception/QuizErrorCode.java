package vn.org.thn.service.app.quiz.exception;

import org.springframework.http.HttpStatus;
import vn.org.thn.service.base.exception.ErrorCode;

/**
 * Error codes specific to quiz-service, on top of {@code CommonErrorCode} in {@code base}
 * (see {@code api-service/README.md} section 4 for the convention: quiz-specific errors get their
 * own enum, generic ones reuse {@code CommonErrorCode}).
 * <p>
 * Role-mismatch on a protected endpoint (e.g. a Student token calling {@code /api/parent/**})
 * deliberately reuses {@code CommonErrorCode.FORBIDDEN} instead of adding a quiz-specific code -
 * "forbidden" is a generic concept, not specific to this service.
 * <p>
 * {@code USERNAME_TAKEN} was added in task 1, for task 2's "create student" endpoint to reuse.
 * {@code LESSON_HAS_QUESTIONS} was defined in task 3 for Lesson's own "delete" endpoint, but
 * could not be wired up yet since {@code Question} did not exist at the time; it was connected
 * in {@code LessonService#delete} once task 4 introduced {@code Question} (see that class's
 * javadoc). Do not confuse it with {@code QUESTION_USED_IN_TEST} (QUIZ_008), which is a
 * different rule guarding Question's own delete endpoint instead.
 */
public enum QuizErrorCode implements ErrorCode {

    UNAUTHORIZED("QUIZ_001", "Unauthorized", HttpStatus.UNAUTHORIZED),
    EMAIL_TAKEN("QUIZ_002", "Email already registered", HttpStatus.CONFLICT),
    USERNAME_TAKEN("QUIZ_003", "Username already taken", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("QUIZ_004", "Invalid email/username or password", HttpStatus.UNAUTHORIZED),
    SUBJECT_HAS_LESSONS("QUIZ_005", "Subject still has lessons - delete its lessons first", HttpStatus.CONFLICT),
    LESSON_HAS_QUESTIONS("QUIZ_006", "Lesson still has questions - delete its questions first", HttpStatus.CONFLICT),
    QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE("QUIZ_007", "A question must have exactly one correct choice", HttpStatus.BAD_REQUEST),
    QUESTION_USED_IN_TEST("QUIZ_008", "Question is already used in a test - delete the test(s) referencing it first", HttpStatus.CONFLICT),
    TEST_HAS_ATTEMPTS("QUIZ_009", "Test already has an attempt - it can no longer be deleted", HttpStatus.CONFLICT),
    ATTEMPT_ALREADY_SUBMITTED("QUIZ_010", "This attempt was already submitted", HttpStatus.CONFLICT),
    IMPORT_TOO_MANY_ROWS("QUIZ_011", "Import file has too many rows", HttpStatus.BAD_REQUEST),
    IMPORT_FILE_UNREADABLE("QUIZ_012", "Import file could not be read - check the format and template", HttpStatus.BAD_REQUEST),
    ATTEMPT_NOT_SUBMITTED("QUIZ_013", "This attempt has not been submitted yet", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    QuizErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus.value();
    }
}
