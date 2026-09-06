package vn.org.thn.service.app.quiz.dto;

import lombok.Data;

import java.util.List;

/**
 * Response body for the bulk-delete endpoints added 2026-09-06 ({@code DELETE
 * /api/parent/{questions,lessons,subjects,classrooms}}) - a best-effort per-id result, same shape
 * as {@link QuestionImportResponse}: one id failing (already deleted by someone else, belongs to
 * another parent, ...) never blocks the rest of the request.
 */
@Data
public class BulkDeleteResponse {
    private int requested;
    private int deletedCount;
    private List<BulkDeleteError> errors;
}
