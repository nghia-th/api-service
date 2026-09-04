package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Admin;

/** Safe response view of {@link Admin} - deliberately excludes {@code password}, same reasoning as {@link ParentResponse}. */
@Data
public class AdminResponse {
    private Long id;
    private String fullName;
    private String email;

    public static AdminResponse from(Admin admin) {
        AdminResponse response = new AdminResponse();
        response.id = admin.getId();
        response.fullName = admin.getFullName();
        response.email = admin.getEmail();
        return response;
    }
}
