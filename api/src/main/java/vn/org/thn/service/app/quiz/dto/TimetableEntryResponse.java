package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.entity.TimetableEntry;

/**
 * One row of a Classroom's weekly timetable, with the Subject's name already resolved so the
 * frontend never needs a second round-trip per entry - same "embed the display fields the UI
 * needs directly" reasoning as {@code SubjectLibraryLinkResponse} embedding a full {@code
 * LibraryDocumentResponse}.
 * <p>
 * No more {@code lessonId}/{@code lessonName} (2026-09-06 revision - see {@code
 * TimetableEntry}'s javadoc): a timetable entry is Subject-level only now.
 */
@Data
public class TimetableEntryResponse {

    private Long id;
    private Integer dayOfWeek;
    private Long subjectId;
    private String subjectName;
    private Integer orderIndex;

    public static TimetableEntryResponse from(TimetableEntry entry, Subject subject) {
        TimetableEntryResponse response = new TimetableEntryResponse();
        response.setId(entry.getId());
        response.setDayOfWeek(entry.getDayOfWeek());
        response.setSubjectId(subject == null ? null : subject.getId());
        response.setSubjectName(subject == null ? null : subject.getName());
        response.setOrderIndex(entry.getOrderIndex());
        return response;
    }
}
