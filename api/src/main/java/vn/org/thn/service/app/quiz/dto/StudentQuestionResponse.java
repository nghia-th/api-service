package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;

import java.util.List;

/**
 * {@code lessonId} was added 2026-09-01 so a Student can fetch the lesson's own content/image
 * (summary/content/textbookPage/image - see {@code StudentLessonApi}) to review the material a
 * question came from, both while taking the test and after submitting - see {@code
 * StudentLessonService}'s javadoc for the access rule this relies on (a lesson is reachable
 * precisely because a question like this one, on a test assigned to this student, belongs to it).
 */
@Data
public class StudentQuestionResponse {
    private Long questionId;
    private Long lessonId;
    private String content;
    private List<StudentChoiceResponse> choices;

    public static StudentQuestionResponse from(Question question, List<Choice> choices) {
        StudentQuestionResponse response = new StudentQuestionResponse();
        response.questionId = question.getId();
        response.lessonId = question.getLessonId();
        response.content = question.getContent();
        response.choices = choices.stream().map(StudentChoiceResponse::from).toList();
        return response;
    }
}
