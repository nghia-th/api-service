package vn.org.thn.service.app.quiz.dto;

import lombok.Data;

import java.util.List;

/** Response body for {@code POST /api/parent/lessons/import} - same best-effort per-row result shape as {@link QuestionImportResponse} (task 4's precedent), never a single pass/fail for the whole file. */
@Data
public class LessonImportResponse {
    private int totalRows;
    private int successCount;
    private List<ImportRowError> errors;
}
