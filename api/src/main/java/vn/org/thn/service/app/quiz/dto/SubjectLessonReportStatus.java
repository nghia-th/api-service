package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * The Student's "bao bai" (2026-09-06) picker for one Subject on today's timetable - one entry
 * per {@code GET /api/student/lesson-reports/today} row. {@code reportedToday} lists the Lessons
 * of this Subject already confirmed studied TODAY (each still undoable, see {@code
 * LessonReportService#unreportLesson}); {@code available} lists this Subject's remaining Lessons
 * that have NEVER been reported by this Student on any date - once a Lesson is reported it drops
 * out of {@code available} forever (AskUserQuestion 2026-09-06: "an vinh vien"), which is exactly
 * what keeps the picker short as the school year progresses through "Bai 1".."Bai 100".
 */
@Data
@AllArgsConstructor
public class SubjectLessonReportStatus {
    private Long subjectId;
    private String subjectName;
    private Integer orderIndex;
    private List<LessonReportCandidate> reportedToday;
    private List<LessonReportCandidate> available;
}
