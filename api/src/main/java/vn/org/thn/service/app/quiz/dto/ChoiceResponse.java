package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Choice;

/**
 * Includes {@code correct} - unlike the student-facing choice view in task 6, this DTO is only
 * ever returned from {@code /api/parent/**} endpoints, where showing the parent the correct
 * answer is expected (they authored the question).
 */
@Data
public class ChoiceResponse {
    private Long id;
    private String content;
    private Boolean correct;

    public static ChoiceResponse from(Choice choice) {
        ChoiceResponse response = new ChoiceResponse();
        response.id = choice.getId();
        response.content = choice.getContent();
        response.correct = choice.getCorrect();
        return response;
    }
}
