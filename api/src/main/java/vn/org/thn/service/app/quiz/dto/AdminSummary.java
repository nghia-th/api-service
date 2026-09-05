package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Admin;

import java.time.LocalDateTime;

/**
 * One row of {@code GET /api/admin/admins} (2026-09-05) - like {@link AdminResponse} (excludes
 * {@code password}) plus {@code createdAt}, same "Admin-facing list view adds the fields only an
 * Admin needs to see" shape as {@link AdminParentSummary}. {@code root} is already on {@link
 * AdminResponse} too (needed there for {@code quizProfile} at login) - repeated here so the list
 * table can show which row is the protected bootstrap account without a second lookup.
 */
@Data
public class AdminSummary {
    private Long id;
    private String fullName;
    private String email;
    private boolean root;
    private LocalDateTime createdAt;

    public static AdminSummary from(Admin admin) {
        AdminSummary response = new AdminSummary();
        response.id = admin.getId();
        response.fullName = admin.getFullName();
        response.email = admin.getEmail();
        response.root = admin.isRoot();
        response.createdAt = admin.getCreatedAt();
        return response;
    }
}
