package vn.org.thn.service.app.quiz.dto;

import lombok.Data;

import java.util.List;

/** Response body for {@code POST /api/parent/subjects/import} - same best-effort per-row result shape as {@link QuestionImportResponse}/{@link LessonImportResponse}/{@link LibraryImportResponse}, never a single pass/fail for the whole file. */
@Data
public class SubjectImportResponse {
    private int totalRows;
    private int successCount;
    private List<ImportRowError> errors;
}
