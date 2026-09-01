package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One failed row from {@code POST /api/parent/questions/import} - see {@code QuestionImportResponse}. */
@Data
@AllArgsConstructor
public class ImportRowError {
    /** 1-based row number as it appears in the uploaded file, including the header row (so the first data row is 2). */
    private int rowNumber;
    private String reason;
}
