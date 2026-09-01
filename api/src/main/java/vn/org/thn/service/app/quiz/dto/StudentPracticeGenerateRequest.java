package vn.org.thn.service.app.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body of {@code POST /api/student/tests/practice} - self-service version of {@link
 * PracticeGenerateRequest}: no {@code studentId} field, the currently logged-in Student always
 * generates a practice Test for themselves (see {@code StudentAttemptService#generatePractice}).
 */
@Data
public class StudentPracticeGenerateRequest {

    @NotNull
    private Long subjectId;

    /** Optional - blank/null defaults to "Ôn tập &lt;tên môn&gt;" (see {@code TestService}). */
    private String name;

    /** Optional - number of random questions to pick; null/non-positive defaults to 10 (or fewer if the Subject has fewer than 10 Questions total), see {@code TestService}. */
    private Integer questionCount;
}
