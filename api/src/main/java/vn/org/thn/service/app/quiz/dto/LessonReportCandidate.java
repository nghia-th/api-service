package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * One Lesson shown in a "bao bai" (2026-09-06) picker - either already reported today, or still
 * available to report. See {@link SubjectLessonReportStatus}'s javadoc.
 */
@Data
@AllArgsConstructor
public class LessonReportCandidate {
    private Long lessonId;
    private String lessonName;
}
