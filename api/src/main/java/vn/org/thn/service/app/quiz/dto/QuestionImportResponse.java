package vn.org.thn.service.app.quiz.dto;

import lombok.Data;

import java.util.List;

/** Response body for {@code POST /api/parent/questions/import} - a best-effort per-row result, never a single pass/fail for the whole file. */
@Data
public class QuestionImportResponse {
    private int totalRows;
    private int successCount;
    private List<ImportRowError> errors;
}
