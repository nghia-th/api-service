package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.entity.TimetableEntry;

/**
 * One row of a Classroom's weekly timetable, with the Lesson's and Subject's names already
 * resolved so the frontend never needs a second round-trip per entry - same "embed the display
 * fields the UI needs directly" reasoning as {@code SubjectLibraryLinkResponse} embedding a full
 * {@code LibraryDocumentResponse}.
 */
@Data
public class TimetableEntryResponse {

    private Long id;
    private Integer dayOfWeek;
    private Long lessonId;
    private String lessonName;
    private Long subjectId;
    private String subjectName;
    private Integer orderIndex;

    public static TimetableEntryResponse from(TimetableEntry entry, Lesson lesson, Subject subject) {
        TimetableEntryResponse response = new TimetableEntryResponse();
        response.setId(entry.getId());
        response.setDayOfWeek(entry.getDayOfWeek());
        response.setLessonId(entry.getLessonId());
        response.setLessonName(lesson == null ? null : lesson.getName());
        response.setSubjectId(subject == null ? null : subject.getId());
        response.setSubjectName(subject == null ? null : subject.getName());
        response.setOrderIndex(entry.getOrderIndex());
        return response;
    }
}
