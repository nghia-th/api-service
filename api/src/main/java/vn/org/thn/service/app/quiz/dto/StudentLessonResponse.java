package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Lesson;

/**
 * Student-facing view of a Lesson's content - deliberately a separate DTO from {@link
 * LessonResponse} (not just reused) so the Parent-facing shape (which carries {@code subjectId}
 * for the Parent's own Subject->Lesson navigation) can evolve independently of what a Student is
 * shown, even though both currently expose the same content fields.
 */
@Data
public class StudentLessonResponse {
    private Long id;
    private String name;
    private String summary;
    private String content;
    private Integer textbookPage;
    private boolean hasImage;

    public static StudentLessonResponse from(Lesson lesson) {
        StudentLessonResponse response = new StudentLessonResponse();
        response.id = lesson.getId();
        response.name = lesson.getName();
        response.summary = lesson.getSummary();
        response.content = lesson.getContent();
        response.textbookPage = lesson.getTextbookPage();
        response.hasImage = lesson.getImagePath() != null;
        return response;
    }
}
