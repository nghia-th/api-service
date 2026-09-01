package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Choice;
import vn.org.thn.service.app.quiz.entity.Question;

import java.util.List;

/**
 * {@code hasAudio} is derived from {@link Question#getAudioPath()} being non-null rather than
 * exposing the raw storage path/filename to clients - the actual bytes are fetched separately via
 * {@code GET /api/parent/questions/{id}/audio}, same pattern {@link LessonResponse#isHasImage()}
 * already uses for a Lesson's illustrative image. {@code hideContentInTest} is passed through as-
 * is here (Parent-facing view - the Parent is always allowed to see their own setting); compare
 * {@link vn.org.thn.service.app.quiz.dto.StudentQuestionResponse}, which enforces the hiding
 * itself instead of merely exposing the flag.
 */
@Data
public class QuestionResponse {
    private Long id;
    private Long lessonId;
    private String content;
    private String knowledgeTag;
    private List<ChoiceResponse> choices;
    private boolean hasAudio;
    private boolean hideContentInTest;
    /** {@link vn.org.thn.service.app.quiz.entity.QuestionType#name()} - "MULTIPLE_CHOICE" or "SPEAKING", added 2026-09-01. Never null - {@code Question#getQuestionType()} is always set (see QuestionService#newQuestion). */
    private String questionType;

    public static QuestionResponse from(Question question, List<Choice> choices) {
        QuestionResponse response = new QuestionResponse();
        response.id = question.getId();
        response.lessonId = question.getLessonId();
        response.content = question.getContent();
        response.knowledgeTag = question.getKnowledgeTag();
        response.choices = choices.stream().map(ChoiceResponse::from).toList();
        response.hasAudio = question.getAudioPath() != null;
        response.hideContentInTest = Boolean.TRUE.equals(question.getHideContentInTest());
        response.questionType = question.getQuestionType();
        return response;
    }
}
