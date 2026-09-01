package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Test;

/** {@code GET /api/student/tests} row - deliberately excludes parentId (not the student's business) and any question content (see {@link StudentQuestionResponse} for that, only returned once the student starts the test). */
@Data
public class StudentTestSummaryResponse {
    private Long id;
    private String name;
    private String status;

    public static StudentTestSummaryResponse from(Test test) {
        StudentTestSummaryResponse response = new StudentTestSummaryResponse();
        response.id = test.getId();
        response.name = test.getName();
        response.status = test.getStatus();
        return response;
    }
}
