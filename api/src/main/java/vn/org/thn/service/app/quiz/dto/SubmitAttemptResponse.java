package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Attempt;

/** Response body for {@code POST /api/student/attempts/{attemptId}/submit} - the basic score view for the student; the knowledge-tag breakdown is Parent-facing only, see task 7's {@code AttemptReportResponse}. */
@Data
public class SubmitAttemptResponse {
    private Long attemptId;
    private Integer correctCount;
    private Integer totalQuestions;
    private Double scorePercent;

    public static SubmitAttemptResponse from(Attempt attempt) {
        SubmitAttemptResponse response = new SubmitAttemptResponse();
        response.attemptId = attempt.getId();
        response.correctCount = attempt.getCorrectCount();
        response.totalQuestions = attempt.getTotalQuestions();
        response.scorePercent = attempt.getTotalQuestions() == null || attempt.getTotalQuestions() == 0
                ? 0.0
                : attempt.getCorrectCount() * 100.0 / attempt.getTotalQuestions();
        return response;
    }
}
