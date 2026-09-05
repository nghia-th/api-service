package vn.org.thn.service.app.quiz.dto;

import lombok.Data;

import java.util.List;

/** Response body for {@code POST /api/admin/library/import} - same best-effort per-row result shape as {@link QuestionImportResponse}/{@link LessonImportResponse} (task 4's precedent), never a single pass/fail for the whole file. */
@Data
public class LibraryImportResponse {
    private int totalRows;
    private int successCount;
    private List<ImportRowError> errors;
}
