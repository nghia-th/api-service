package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Choice;

/**
 * The Student-facing view of a {@link Choice} - deliberately has NO {@code correct} field, unlike
 * {@link ChoiceResponse} (task 4, Parent-facing). This is the single most important thing to get
 * right in task 6 per its own acceptance criteria: never map {@link Choice} straight onto a DTO
 * that carries {@code correct} on this path.
 */
@Data
public class StudentChoiceResponse {
    private Long choiceId;
    private String content;

    public static StudentChoiceResponse from(Choice choice) {
        StudentChoiceResponse response = new StudentChoiceResponse();
        response.choiceId = choice.getId();
        response.content = choice.getContent();
        return response;
    }
}
