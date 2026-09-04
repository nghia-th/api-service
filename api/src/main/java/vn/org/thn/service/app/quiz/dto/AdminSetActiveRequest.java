package vn.org.thn.service.app.quiz.dto;

import lombok.Data;

/** Request body for {@code PATCH /api/admin/parents/{id}/active}. */
@Data
public class AdminSetActiveRequest {
    private boolean active;
}
