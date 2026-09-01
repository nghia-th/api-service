package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code PUT
 * /api/student/attempts/{attemptId}/questions/{questionId}/speaking-answer/text} (2026-09-01,
 * typed-essay alternative to voice recording for a SPEAKING question - see {@code AnswerMode}'s
 * javadoc). {@code text} null or blank CLEARS the Student's saved typed answer back to nothing
 * (same "send blank to clear" shape as every other free-text field in this codebase) - there is no
 * separate DELETE endpoint for this, unlike the recorded-audio answer, since a plain text field is
 * simple enough to just overwrite in place.
 */
@Data
public class SpeakingTextAnswerRequest {

    @Size(max = 10000)
    @Schema(type = "string", description = "The Student's typed answer. Null/blank clears it back to unanswered.", nullable = true)
    private String text;
}
