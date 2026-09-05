package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One row of the "prepared for tomorrow" checklist - merges one {@code TimetableEntry} (via
 * {@link vn.org.thn.service.app.quiz.service.TimetableService#getForClassroomAndDate}) with
 * whether a {@code LessonPreparation} row exists for it, for BOTH the Student's own view (item 9)
 * and the Parent's read-only view of a student (item 10) - same shape either way, only {@code
 * prepared} ever differs by viewer.
 */
@Data
@AllArgsConstructor
public class LessonPreparationStatus {
    private Long lessonId;
    private String lessonName;
    private Long subjectId;
    private String subjectName;
    private Integer orderIndex;
    private boolean prepared;
}
