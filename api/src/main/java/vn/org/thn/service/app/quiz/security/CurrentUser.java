package vn.org.thn.service.app.quiz.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.base.exception.BusinessException;

/**
 * The authenticated caller (userId + role) for the current request, populated into a request
 * attribute by {@link JwtAuthFilter} and read back here via {@link #get()}.
 * <p>
 * This is deliberately a standalone helper over {@link RequestContextHolder}, not a {@code
 * BaseCtl} method like {@code getClientIp()} - it must be callable from the Service layer too
 * (every {@code *Service} method that touches Parent/Student-owned data calls this to filter its
 * query by {@code userId()}/role, per {@code docs/01-thiet-ke-tong-the.md} section 3, auth), and Services
 * don't extend {@code BaseCtl}.
 */
public record CurrentUser(Long userId, Role role) {

    /** Request attribute key {@link JwtAuthFilter} stores the resolved caller under. */
    public static final String REQUEST_ATTRIBUTE = "quiz.currentUser";

    /**
     * The current request's authenticated caller.
     *
     * @throws BusinessException(QuizErrorCode.UNAUTHORIZED) if called with no request in scope,
     *                                                        or before {@link JwtAuthFilter} has verified a token for it (should never
     *                                                        happen for any endpoint under {@code /api/parent/**}/{@code /api/student/**} -
     *                                                        the filter rejects the request first in that case).
     */
    public static CurrentUser get() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            throw new BusinessException(QuizErrorCode.UNAUTHORIZED, "No request in scope");
        }
        Object value = attrs.getRequest().getAttribute(REQUEST_ATTRIBUTE);
        if (!(value instanceof CurrentUser currentUser)) {
            throw new BusinessException(QuizErrorCode.UNAUTHORIZED, "No authenticated user for this request");
        }
        return currentUser;
    }
}
