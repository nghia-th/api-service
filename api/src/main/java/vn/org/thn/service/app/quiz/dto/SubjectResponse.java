package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Subject;

@Data
public class SubjectResponse {
    private Long id;
    private Long parentId;
    private String name;

    public static SubjectResponse from(Subject subject) {
        SubjectResponse response = new SubjectResponse();
        response.id = subject.getId();
        response.parentId = subject.getParentId();
        response.name = subject.getName();
        return response;
    }
}
