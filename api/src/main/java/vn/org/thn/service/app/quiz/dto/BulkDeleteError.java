package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One failed id from a bulk-delete endpoint (Question/Lesson/Subject/Classroom) - see {@link BulkDeleteResponse}. Same shape as {@link ImportRowError}, id instead of a row number. */
@Data
@AllArgsConstructor
public class BulkDeleteError {
    private Long id;
    private String reason;
}
