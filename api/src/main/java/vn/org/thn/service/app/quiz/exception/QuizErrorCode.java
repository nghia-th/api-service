package vn.org.thn.service.app.quiz.exception;

import org.springframework.http.HttpStatus;
import vn.org.thn.service.base.exception.ErrorCode;

/**
 * Error codes specific to quiz-service, on top of {@code CommonErrorCode} in {@code base}
 * (see {@code api-service/README.md} muc 4 for the convention: quiz-specific errors get their
 * own enum, generic ones reuse {@code CommonErrorCode}).
 * <p>
 * Role-mismatch on a protected endpoint (e.g. a Student token calling {@code /api/parent/**})
 * deliberately reuses {@code CommonErrorCode.FORBIDDEN} instead of adding a quiz-specific code -
 * "forbidden" is a generic concept, not specific to this service.
 * <p>
 * {@code USERNAME_TAKEN} was added in task 1, for task 2's "create student" endpoint to reuse.
 * {@code LESSON_HAS_QUESTIONS} is not used yet (task 4, once {@code Question} exists) - added now
 * next to {@code SUBJECT_HAS_LESSONS} so both "still has children" errors are defined together.
 */
public enum QuizErrorCode implements ErrorCode {

    UNAUTHORIZED("QUIZ_001", "Unauthorized", HttpStatus.UNAUTHORIZED),
    EMAIL_TAKEN("QUIZ_002", "Email already registered", HttpStatus.CONFLICT),
    USERNAME_TAKEN("QUIZ_003", "Username already taken", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("QUIZ_004", "Invalid email/username or password", HttpStatus.UNAUTHORIZED),
    SUBJECT_HAS_LESSONS("QUIZ_005", "Subject still has lessons - delete its lessons first", HttpStatus.CONFLICT),
    LESSON_HAS_QUESTIONS("QUIZ_006", "Lesson still has questions - delete its questions first", HttpStatus.CONFLICT);

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
