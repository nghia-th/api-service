package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response body for {@code GET /api/student/tests/{testId}/answers} (added 2026-09-02, "xem lai
 * dap an nhung de da lam" per the user's request) - the current Student's own read-only review of
 * one already-submitted attempt. Same shape as the Parent-facing {@link AttemptReportResponse}
 * (task 7) minus {@code studentName} (always the caller's own, no need to say so) and using
 * {@link StudentAttemptAnswerDetail} rows instead (no {@code referenceAnswer} - see that class's
 * javadoc).
 */
@Data
@AllArgsConstructor
public class StudentAttemptReportResponse {
    private Long attemptId;
    private String testName;
    private String testType;
    private Integer correctCount;
    private Integer totalQuestions;
    private Double scorePercent;
    private LocalDateTime submittedAt;
    private List<StudentAttemptAnswerDetail> answers;
    private List<KnowledgeTagBreakdown> byKnowledgeTag;
}
