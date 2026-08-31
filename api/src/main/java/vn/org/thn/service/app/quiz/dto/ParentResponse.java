package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Parent;

/** Safe response view of {@link Parent} - deliberately excludes {@code password}, unlike returning the entity directly. */
@Data
public class ParentResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;

    public static ParentResponse from(Parent parent) {
        ParentResponse response = new ParentResponse();
        response.id = parent.getId();
        response.fullName = parent.getFullName();
        response.email = parent.getEmail();
        response.phone = parent.getPhone();
        return response;
    }
}
