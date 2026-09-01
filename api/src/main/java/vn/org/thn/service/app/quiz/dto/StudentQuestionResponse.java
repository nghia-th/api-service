package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.AnswerMode;
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
 * <p>
 * {@code answerMode}/{@code answerText} were added 2026-09-01 for the typed-essay alternative to
 * voice recording (see {@code AnswerMode}'s javadoc). {@code answerMode} tells the client which
 * control(s) to render (record button, text box, or both) - never null, defaults to AUDIO for
 * MULTIPLE_CHOICE questions and pre-2026-09-01 SPEAKING questions alike (irrelevant for the former,
 * simply unused by the client). {@code answerText} is the Student's OWN already-saved typed
 * answer, if any - included here (not fetched lazily like the recorded-audio's blob) because it is
 * plain text, cheap to send eagerly, and lets the take-test screen show it immediately on resume
 * without a background prefetch call per question (contrast the audio answer, which still needs
 * its own GET for playback bytes).
 */
@Data
public class StudentQuestionResponse {
    private Long questionId;
    private Long lessonId;
    private String content;
    private boolean hasAudio;
    private List<StudentChoiceResponse> choices;
    /** {@link vn.org.thn.service.app.quiz.entity.QuestionType#name()}, added 2026-09-01 - tells the client whether to render a choice list (MULTIPLE_CHOICE) or a voice recorder (SPEAKING, choices is always empty for these). */
    private String questionType;
    /** {@link AnswerMode#name()}, added 2026-09-01 - AUDIO/TEXT/BOTH, see AnswerMode's javadoc. Only meaningful when questionType is SPEAKING. */
    private String answerMode;
    /** The Student's own already-saved typed answer for this question (2026-09-01), null if none saved yet or questionType is not SPEAKING. */
    private String answerText;

    public static StudentQuestionResponse from(Question question, List<Choice> choices, String answerText) {
        StudentQuestionResponse response = new StudentQuestionResponse();
        response.questionId = question.getId();
        response.lessonId = question.getLessonId();
        response.hasAudio = question.getAudioPath() != null;
        boolean hideContent = response.hasAudio && Boolean.TRUE.equals(question.getHideContentInTest());
        response.content = hideContent ? null : question.getContent();
        response.choices = choices.stream().map(StudentChoiceResponse::from).toList();
        response.questionType = question.getQuestionType();
        response.answerMode = question.getAnswerMode() == null ? AnswerMode.AUDIO.name() : question.getAnswerMode();
        response.answerText = answerText;
        return response;
    }
}
