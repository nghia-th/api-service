package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One row of {@link StudentAttemptReportResponse#getAnswers()} - {@code GET
 * /api/student/tests/{testId}/answers} (added 2026-09-02, "xem lai dap an nhung de da lam" per the
 * user's request). Same shape as the Parent-facing {@link AttemptAnswerDetail} (task 7) EXCEPT it
 * deliberately drops {@code referenceAnswer} - that field is the Parent's own private note entered
 * at question-authoring time ({@code Question#referenceAnswer}), never meant for the Student to
 * read (same "never leak this to the Student" reasoning that already keeps {@code Choice#correct}
 * off {@link StudentChoiceResponse} while a test is in progress).
 * <p>
 * Only ever populated for an ALREADY-SUBMITTED attempt (see {@code
 * StudentAttemptService#getOwnAttemptReport}) - unlike the in-progress view ({@link
 * StudentQuestionResponse}), this DOES include {@code correctChoiceContent}/{@code correct}, same
 * as the Parent's report, because there is no more cheating risk once the attempt is locked and
 * graded.
 * <p>
 * {@code hasAudio}/{@code hasVideo} (2026-09-06, "phan xem dap an cua hoc sinh anh muon hien thi
 * them phan audio, video ma cau hoi co") - same "flag here / bytes via their own endpoint" split
 * as {@link StudentQuestionResponse#isHasAudio()}/{@code isHasVideo()}, reusing the SAME {@code
 * GET /api/student/questions/{id}/audio}/{@code .../video} endpoints - those already allow access
 * to any Question on any Test ever assigned to this Student regardless of attempt status (see
 * {@code StudentAttemptService#getQuestionAudio}/{@code #getQuestionVideo}'s own access check), so
 * no backend endpoint change was needed to make review-mode playback work - only exposing the two
 * flags here.
 */
@Data
@AllArgsConstructor
public class StudentAttemptAnswerDetail {
    private Long questionId;
    private String questionContent;
    /** Null if the student left this question blank. */
    private String chosenChoiceContent;
    private String correctChoiceContent;
    private boolean correct;
    private String knowledgeTag;
    private String questionType;
    private boolean hasSpeakingAnswer;
    private Boolean parentMarkedCorrect;
    private String answerText;
    private String answerMode;
    private boolean hasAudio;
    private boolean hasVideo;
}
