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
 * different rule guarding Question's own delete endpoint instead. {@code QUESTION_HAS_ATTEMPTS}
 * (QUIZ_019) is a THIRD, separate rule again, guarding Question's own UPDATE endpoint - see
 * {@code QuestionService#update}'s javadoc. {@code QUESTION_AUDIO_INVALID_TYPE}/{@code
 * QUESTION_AUDIO_TOO_LARGE} (QUIZ_020/QUIZ_021) guard {@code QuestionService#uploadAudio}, same
 * shape as {@code LESSON_IMAGE_INVALID_TYPE}/{@code LESSON_IMAGE_TOO_LARGE} (QUIZ_016/QUIZ_017)
 * for Lesson's illustrative image. {@code QUESTION_CHOICES_REQUIRED} (QUIZ_022, added 2026-09-01
 * for the "speaking question" feature) replaces the removed {@code @Size(min = 2)} bean
 * validation on {@code QuestionRequest#choices} - that annotation could not be conditional on
 * {@code questionType}, so the "at least 2 choices" rule moved into {@code
 * QuestionService#validateChoices}, MULTIPLE_CHOICE only. {@code SPEAKING_ANSWER_INVALID_TYPE}/
 * {@code SPEAKING_ANSWER_TOO_LARGE} (QUIZ_023/QUIZ_024) guard {@code
 * StudentAttemptService#uploadSpeakingAnswer}, same shape as QUIZ_020/QUIZ_021 for Question audio.
 * {@code QUESTION_NOT_SPEAKING_TYPE} (QUIZ_025) guards every speaking-answer/grade endpoint
 * against being called on an ordinary MULTIPLE_CHOICE question. {@code REFRESH_TOKEN_INVALID}
 * (QUIZ_026, added 2026-09-04 for the refresh-token feature) is ONE shared error for "unknown
 * token" / "already revoked" / "expired" / "owning account no longer exists" - same "never reveal
 * which" reasoning as {@code INVALID_CREDENTIALS}, see {@code AuthService#findValidRefreshTokenOrThrow}.
 * {@code ACCOUNT_DEACTIVATED} (QUIZ_027, added 2026-09-04 for the Admin feature) is thrown by
 * {@code AuthService#loginParent}/{@code #loginStudent} ONLY after the password already matched
 * (never before - an unauthenticated caller must not learn "this email exists but is
 * deactivated" from a wrong-password guess) - see {@code entity/Parent.java#active}'s javadoc.
 * {@code QUESTION_VIDEO_INVALID_TYPE}/{@code QUESTION_VIDEO_TOO_LARGE} (QUIZ_028/QUIZ_029, added
 * 2026-09-04 for the "video question" feature, part 3/4) guard {@code
 * QuestionService#uploadVideo}, same shape as {@code QUESTION_AUDIO_INVALID_TYPE}/{@code
 * QUESTION_AUDIO_TOO_LARGE} (QUIZ_020/QUIZ_021) for a question's audio clip. {@code
 * OLD_PASSWORD_INCORRECT} (QUIZ_030, added 2026-09-04 for the self-service change-password
 * feature) guards {@code AuthService#changePassword} - deliberately its OWN code rather than
 * reusing {@code INVALID_CREDENTIALS} (QUIZ_004), since this happens to an ALREADY-authenticated
 * caller (wrong old password on a change-password call), a different situation from a failed
 * login. {@code ROOT_ADMIN_CANNOT_BE_DELETED} (QUIZ_031, added 2026-09-05 for the Admin-manages-
 * Admin feature - "root la tai khoan cao nhat, chi root xoa duoc Admin khac, khong ai xoa duoc
 * root") guards {@code AdminManageService#delete} - deliberately its OWN code rather than reusing
 * {@code CommonErrorCode.FORBIDDEN} (used by that same method for the OTHER rule, "caller is not
 * root"): this is a different failure reason (caller IS root, but the TARGET row is the protected
 * one), so the frontend/log can tell the two apart.
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
    ATTEMPT_NOT_SUBMITTED("QUIZ_013", "This attempt has not been submitted yet", HttpStatus.CONFLICT),
    CLASSROOM_HAS_STUDENTS("QUIZ_014", "Classroom still has students - move or delete them first", HttpStatus.CONFLICT),
    CLASSROOM_HAS_SUBJECTS("QUIZ_015", "Classroom still has subjects - delete them first", HttpStatus.CONFLICT),
    LESSON_IMAGE_INVALID_TYPE("QUIZ_016", "Lesson image must be a JPEG, PNG or WebP file", HttpStatus.BAD_REQUEST),
    LESSON_IMAGE_TOO_LARGE("QUIZ_017", "Lesson image must be 5MB or smaller", HttpStatus.BAD_REQUEST),
    SUBJECT_NO_QUESTIONS("QUIZ_018", "This subject has no questions yet - add questions before generating a practice test", HttpStatus.BAD_REQUEST),
    QUESTION_HAS_ATTEMPTS("QUIZ_019", "This question has already been answered in a test attempt - it can no longer be edited", HttpStatus.CONFLICT),
    QUESTION_AUDIO_INVALID_TYPE("QUIZ_020", "Question audio must be an MP3, M4A, WAV or OGG file", HttpStatus.BAD_REQUEST),
    QUESTION_AUDIO_TOO_LARGE("QUIZ_021", "Question audio must be 10MB or smaller", HttpStatus.BAD_REQUEST),
    QUESTION_CHOICES_REQUIRED("QUIZ_022", "A multiple-choice question needs at least 2 choices", HttpStatus.BAD_REQUEST),
    SPEAKING_ANSWER_INVALID_TYPE("QUIZ_023", "Speaking answer must be an MP3, M4A, WAV or OGG file", HttpStatus.BAD_REQUEST),
    SPEAKING_ANSWER_TOO_LARGE("QUIZ_024", "Speaking answer must be 10MB or smaller", HttpStatus.BAD_REQUEST),
    QUESTION_NOT_SPEAKING_TYPE("QUIZ_025", "This question is not a speaking question", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_INVALID("QUIZ_026", "Refresh token is unknown, expired, already used, or its account no longer exists", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DEACTIVATED("QUIZ_027", "This account has been deactivated by an administrator", HttpStatus.FORBIDDEN),
    QUESTION_VIDEO_INVALID_TYPE("QUIZ_028", "Question video must be an MP4, WebM, MOV or OGG file", HttpStatus.BAD_REQUEST),
    QUESTION_VIDEO_TOO_LARGE("QUIZ_029", "Question video must be 50MB or smaller", HttpStatus.BAD_REQUEST),
    OLD_PASSWORD_INCORRECT("QUIZ_030", "Current password is incorrect", HttpStatus.BAD_REQUEST),
    ROOT_ADMIN_CANNOT_BE_DELETED("QUIZ_031", "Root admin account cannot be deleted", HttpStatus.CONFLICT);

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
