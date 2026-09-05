package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * One row of the Parent's read-only "bao bai" history view ({@code GET
 * /api/parent/students/{studentId}/lesson-reports}, item added 2026-09-06) - a flat list for one
 * {@code date} (defaults to today, see {@code LessonReportService#getStudentHistory}), optionally
 * filtered down to one Subject. Both Subject and Lesson names are embedded so the frontend needs
 * no second round-trip, same "embed the display fields the UI needs directly" reasoning as {@code
 * TimetableEntryResponse}.
 */
@Data
@AllArgsConstructor
public class LessonReportHistoryItem {
    private Long subjectId;
    private String subjectName;
    private Long lessonId;
    private String lessonName;
    private LocalDate reportDate;
}
