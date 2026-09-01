package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Classroom;

@Data
public class ClassroomResponse {
    private Long id;
    private Long parentId;
    private String name;

    public static ClassroomResponse from(Classroom classroom) {
        ClassroomResponse response = new ClassroomResponse();
        response.id = classroom.getId();
        response.parentId = classroom.getParentId();
        response.name = classroom.getName();
        return response;
    }
}
