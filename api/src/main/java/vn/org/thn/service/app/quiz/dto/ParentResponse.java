package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Parent;

/**
 * Safe response view of {@link Parent} - deliberately excludes {@code password}, unlike returning
 * the entity directly. {@code username} (2026-09-05) is the optional alternate login identifier,
 * see {@link Parent#getUsername()}'s javadoc.
 */
@Data
public class ParentResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String username;

    public static ParentResponse from(Parent parent) {
        ParentResponse response = new ParentResponse();
        response.id = parent.getId();
        response.fullName = parent.getFullName();
        response.email = parent.getEmail();
        response.phone = parent.getPhone();
        response.username = parent.getUsername();
        return response;
    }
}
