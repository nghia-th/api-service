package vn.org.thn.service.app.quiz.security;

/**
 * The account types in quiz-service, carried in the JWT's "role" claim and used to gate {@code
 * /api/parent/**}/{@code /api/student/**}/{@code /api/admin/**} (see {@code JwtAuthFilter}).
 * {@code ADMIN} (2026-09-04) manages Parent accounts (list/create/activate-deactivate/delete) -
 * see {@code AdminParentApi}/{@code AdminParentService} - there is no Admin self-registration,
 * see {@code config/AdminBootstrapRunner}.
 */
public enum Role {
    PARENT,
    STUDENT,
    ADMIN
}
