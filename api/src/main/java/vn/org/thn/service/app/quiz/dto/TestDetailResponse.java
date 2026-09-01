package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Test;

import java.util.List;

/** {@link TestResponse} plus the full, ordered list of questions - only returned by {@code GET /api/parent/tests/{id}}, not the list endpoint (see task 5 spec: the list endpoint is a summary). */
@Data
public class TestDetailResponse {
    private Long id;
    private Long parentId;
    private Long studentId;
    private String name;
    private String status;
    private List<QuestionResponse> questions;

    public static TestDetailResponse from(Test test, List<QuestionResponse> questions) {
        TestDetailResponse response = new TestDetailResponse();
        response.id = test.getId();
        response.parentId = test.getParentId();
        response.studentId = test.getStudentId();
        response.name = test.getName();
        response.status = test.getStatus();
        response.questions = questions;
        return response;
    }
}
