package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Admin;

/**
 * Safe response view of {@link Admin} - deliberately excludes {@code password}, same reasoning as
 * {@link ParentResponse}.
 * <p>
 * {@code root} (2026-09-05, added for the Admin-manages-Admin feature) - lets the frontend know
 * WITHOUT a second call whether the currently logged-in Admin is the protected bootstrap account
 * (see {@code entity/Admin.java#root}'s javadoc): {@code BlocQuizLogin.ts} stores this whole
 * object as {@code quizProfile} on login, so {@code AppShell.tsx}/{@code Admins.tsx} read {@code
 * quizProfile.root} straight out of localStorage to show/hide the "Quản lý Admin" menu item and
 * guard that route client-side - the REAL enforcement is still server-side, see {@code
 * AdminManageService}'s javadoc.
 */
@Data
public class AdminResponse {
    private Long id;
    private String fullName;
    private String email;
    private boolean root;

    public static AdminResponse from(Admin admin) {
        AdminResponse response = new AdminResponse();
        response.id = admin.getId();
        response.fullName = admin.getFullName();
        response.email = admin.getEmail();
        response.root = admin.isRoot();
        return response;
    }
}
