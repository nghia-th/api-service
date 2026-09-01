package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;

import java.util.List;

@Data
public class QuestionResponse {
    private Long id;
    private Long lessonId;
    private String content;
    private String knowledgeTag;
    private List<ChoiceResponse> choices;

    public static QuestionResponse from(Question question, List<Choice> choices) {
        QuestionResponse response = new QuestionResponse();
        response.id = question.getId();
        response.lessonId = question.getLessonId();
        response.content = question.getContent();
        response.knowledgeTag = question.getKnowledgeTag();
        response.choices = choices.stream().map(ChoiceResponse::from).toList();
        return response;
    }
}
