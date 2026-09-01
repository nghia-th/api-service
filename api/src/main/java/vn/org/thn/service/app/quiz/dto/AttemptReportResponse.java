package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** Response body for {@code GET /api/parent/attempts/{id}} - see task 7 spec for the exact shape. */
@Data
@AllArgsConstructor
public class AttemptReportResponse {
    private Long attemptId;
    private String testName;
    private String studentName;
    private Integer correctCount;
    private Integer totalQuestions;
    private Double scorePercent;
    private LocalDateTime submittedAt;
    private List<AttemptAnswerDetail> answers;
    private List<KnowledgeTagBreakdown> byKnowledgeTag;
}
