package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Lesson;

@Data
public class LessonResponse {
    private Long id;
    private Long subjectId;
    private String name;

    public static LessonResponse from(Lesson lesson) {
        LessonResponse response = new LessonResponse();
        response.id = lesson.getId();
        response.subjectId = lesson.getSubjectId();
        response.name = lesson.getName();
        return response;
    }
}
