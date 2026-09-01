package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request body for {@code PUT
 * /api/parent/attempts/{attemptId}/questions/{questionId}/grade} - the Parent's own reference
 * grade for a SPEAKING answer (task "Cau hoi dang tu luan/thu am", 2026-09-01). Purely for the
 * Parent's own report reading; never affects {@code Attempt.correctCount}/{@code scorePercent} -
 * see {@code AttemptAnswer#parentMarkedCorrect}'s javadoc.
 */
@Data
public class SpeakingGradeRequest {

    @Schema(type = "boolean", example = "true", description = "true = mark correct, false = mark incorrect, null/omitted = clear back to \"not reviewed\"", nullable = true)
    private Boolean correct;
}
