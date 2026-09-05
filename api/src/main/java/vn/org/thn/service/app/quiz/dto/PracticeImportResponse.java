package vn.org.thn.service.app.quiz.dto;

import lombok.Data;

import java.util.List;

/** Response body for {@code POST /api/parent/tests/practice/import} - same best-effort per-row result shape as {@link LessonImportResponse}/{@code QuestionImportResponse}, never a single pass/fail for the whole file. */
@Data
public class PracticeImportResponse {
    private int totalRows;
    private int successCount;
    private List<ImportRowError> errors;
}
