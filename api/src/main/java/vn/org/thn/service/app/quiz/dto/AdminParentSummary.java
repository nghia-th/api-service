package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Parent;

import java.time.LocalDateTime;

/**
 * One row of {@code GET /api/admin/parents} - like {@link ParentResponse} (excludes {@code
 * password}) plus the 2 fields only an Admin needs to see: {@code active} and {@code createdAt}.
 * {@code username} (2026-09-05) is the optional alternate login identifier, shown so the Admin can
 * see which Parents have set one yet.
 */
@Data
public class AdminParentSummary {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private boolean active;
    private LocalDateTime createdAt;

    public static AdminParentSummary from(Parent parent) {
        AdminParentSummary response = new AdminParentSummary();
        response.id = parent.getId();
        response.fullName = parent.getFullName();
        response.email = parent.getEmail();
        response.phone = parent.getPhone();
        response.username = parent.getUsername();
        response.active = parent.isActive();
        response.createdAt = parent.getCreatedAt();
        return response;
    }
}
