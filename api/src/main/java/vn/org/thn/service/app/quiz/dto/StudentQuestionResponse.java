package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;

import java.util.List;

@Data
public class StudentQuestionResponse {
    private Long questionId;
    private String content;
    private List<StudentChoiceResponse> choices;

    public static StudentQuestionResponse from(Question question, List<Choice> choices) {
        StudentQuestionResponse response = new StudentQuestionResponse();
        response.questionId = question.getId();
        response.content = question.getContent();
        response.choices = choices.stream().map(StudentChoiceResponse::from).toList();
        return response;
    }
}
