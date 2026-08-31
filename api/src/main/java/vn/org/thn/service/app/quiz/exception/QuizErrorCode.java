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
 * {@code USERNAME_TAKEN} is not used yet by task 1 (there is no student self-register) - it is
 * added now per the task 1 spec, for task 2's "create student" endpoint to reuse.
 */
public enum QuizErrorCode implements ErrorCode {

    UNAUTHORIZED("QUIZ_001", "Unauthorized", HttpStatus.UNAUTHORIZED),
    EMAIL_TAKEN("QUIZ_002", "Email already registered", HttpStatus.CONFLICT),
    USERNAME_TAKEN("QUIZ_003", "Username already taken", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("QUIZ_004", "Invalid email/username or password", HttpStatus.UNAUTHORIZED);

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
