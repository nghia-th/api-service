package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Student;

/** Safe response view of {@link Student} - deliberately excludes {@code password}. */
@Data
public class StudentResponse {
    private Long id;
    private Long parentId;
    private String fullName;
    private String grade;
    private String username;

    public static StudentResponse from(Student student) {
        StudentResponse response = new StudentResponse();
        response.id = student.getId();
        response.parentId = student.getParentId();
        response.fullName = student.getFullName();
        response.grade = student.getGrade();
        response.username = student.getUsername();
        return response;
    }
}
