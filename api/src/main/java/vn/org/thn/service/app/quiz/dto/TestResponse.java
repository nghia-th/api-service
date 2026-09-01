package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Test;

@Data
public class TestResponse {
    private Long id;
    private Long parentId;
    private Long studentId;
    private String name;
    private String status;
    private String testType;

    public static TestResponse from(Test test) {
        TestResponse response = new TestResponse();
        response.id = test.getId();
        response.parentId = test.getParentId();
        response.studentId = test.getStudentId();
        response.name = test.getName();
        response.status = test.getStatus();
        response.testType = test.getTestType();
        return response;
    }
}
