package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** One row of {@code GET /api/parent/students/{studentId}/attempts} - a rolled-up summary; the per-question breakdown is only in {@link AttemptReportResponse}, fetched separately when the parent taps into one attempt. Only submitted attempts appear here - an in-progress attempt has no submittedAt/correctCount worth showing in a history list (assumption, flagged for review - the task 7 spec does not say this explicitly). */
@Data
@AllArgsConstructor
public class StudentAttemptHistoryItem {
    private Long attemptId;
    private String testName;
    private LocalDateTime submittedAt;
    private Integer correctCount;
    private Integer totalQuestions;
    private String testType;
}
