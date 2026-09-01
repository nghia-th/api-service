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
 * <p>
 * {@code hasAudio}/{@code content} (listening-question feature, same date): {@code hasAudio} tells
 * the client whether to show a play control (audio fetched separately, {@code GET
 * /api/student/questions/{id}/audio}, same "flag here / bytes via their own endpoint" split as
 * {@code hasImage}). When the question has audio AND its {@code Question.hideContentInTest} is
 * true, {@code content} is set to null HERE, server-side - this is the actual enforcement of "make
 * the Student rely on the audio, not the text" (never trust a client-side hide, the same
 * reasoning {@link StudentChoiceResponse} already applies to leaving out {@code correct}). {@code
 * content} is otherwise always the full question text, exactly as today.
 */
@Data
public class StudentQuestionResponse {
    private Long questionId;
    private Long lessonId;
    private String content;
    private boolean hasAudio;
    private List<StudentChoiceResponse> choices;

    public static StudentQuestionResponse from(Question question, List<Choice> choices) {
        StudentQuestionResponse response = new StudentQuestionResponse();
        response.questionId = question.getId();
        response.lessonId = question.getLessonId();
        response.hasAudio = question.getAudioPath() != null;
        boolean hideContent = response.hasAudio && Boolean.TRUE.equals(question.getHideContentInTest());
        response.content = hideContent ? null : question.getContent();
        response.choices = choices.stream().map(StudentChoiceResponse::from).toList();
        return response;
    }
}
