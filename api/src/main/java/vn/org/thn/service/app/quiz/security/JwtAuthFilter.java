package vn.org.thn.service.app.quiz.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.org.thn.service.app.quiz.entity.Admin;
import vn.org.thn.service.app.quiz.entity.Parent;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AdminRepository;
import vn.org.thn.service.app.quiz.repository.ParentRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.base.exception.CommonErrorCode;
import vn.org.thn.service.base.exception.ErrorCode;
import vn.org.thn.service.base.response.ApiResponse;
import vn.org.thn.service.base.util.JsonUtils;

import java.io.IOException;

/**
 * Verifies the {@code Authorization: Bearer <token>} header on every request under {@code
 * /api/parent/*}/{@code /api/student/*}/{@code /api/admin/*} (see registration + URL patterns in
 * {@code config/SecurityConfig}), modeled on {@code base}'s {@code RequestContextFilter} - a plain
 * {@link Filter}, not full Spring Security, kept intentionally simple to match the rest of {@code
 * base}. {@code /api/auth/**} is never matched by this filter's URL patterns, so the register/
 * login/refresh/logout endpoints stay open.
 * <p>
 * On success, stores the resolved {@link CurrentUser} as a request attribute for {@link
 * CurrentUser#get()} to read back in the Service layer. On failure, writes the standard {@link
 * ApiResponse} error envelope directly (this runs before the DispatcherServlet, so {@code
 * GlobalExceptionHandler} never sees a rejection here - it only handles exceptions thrown from
 * inside a controller method).
 * <p>
 * <b>tokenVersion re-check (2026-09-04):</b> a validly-signed, unexpired token is no longer
 * enough on its own - after {@link JwtUtil#parse} succeeds, this filter loads the Parent/Student
 * row by id and compares its current {@code tokenVersion} against the token's {@code tv} claim.
 * This fixes the bug where a deleted account's (or a stale, backend-restart-surviving) token kept
 * working: the row lookup itself fails once the account is gone, and the version compare fails
 * once {@code AuthService#logoutAll()} (force-logout) has bumped it - either way every request
 * after that point is rejected, not just ones after the token's own (now much shorter, see {@link
 * JwtUtil}) natural expiry.
 * <p>
 * <b>RAM cache (2026-09-04):</b> the tokenVersion/active re-check above used to mean a DB read on
 * EVERY single request - now it goes through {@link TokenVersionCache} first (see that
 * interface's javadoc for the full design, including why every mutation site MUST evict rather
 * than rely on a TTL). Only a cache MISS falls through to a repository read.
 */
public class JwtAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String PARENT_PREFIX = "/api/parent/";
    private static final String STUDENT_PREFIX = "/api/student/";
    private static final String ADMIN_PREFIX = "/api/admin/";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final TokenVersionCache tokenVersionCache;

    public JwtAuthFilter(JwtUtil jwtUtil, ParentRepository parentRepository, StudentRepository studentRepository,
                          AdminRepository adminRepository, TokenVersionCache tokenVersionCache) {
        this.jwtUtil = jwtUtil;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.tokenVersionCache = tokenVersionCache;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Role requiredRole = requiredRoleFor(httpRequest.getRequestURI());
        if (requiredRole == null) {
            // Defensive only: with the URL patterns this filter is registered against
            // (/api/parent/*, /api/student/*), every request reaching here already matches one.
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(httpRequest);
        if (token == null) {
            writeError(httpResponse, QuizErrorCode.UNAUTHORIZED, "Missing or malformed Authorization header");
            return;
        }

        JwtUtil.Payload payload;
        try {
            payload = jwtUtil.parse(token);
        } catch (ExpiredJwtException e) {
            writeError(httpResponse, QuizErrorCode.UNAUTHORIZED, "Token expired");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected invalid token on {}: {}", httpRequest.getRequestURI(), e.getMessage());
            writeError(httpResponse, QuizErrorCode.UNAUTHORIZED, "Invalid token");
            return;
        }

        if (payload.role() != requiredRole) {
            writeError(httpResponse, CommonErrorCode.FORBIDDEN,
                    "Token role " + payload.role() + " cannot access " + httpRequest.getRequestURI());
            return;
        }

        if (!currentTokenVersionMatches(payload)) {
            writeError(httpResponse, QuizErrorCode.UNAUTHORIZED,
                    "Token no longer valid - the account was removed, or logged out from all devices");
            return;
        }

        httpRequest.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, new CurrentUser(payload.userId(), payload.role()));
        chain.doFilter(request, response);
    }

    /**
     * Re-checks {@code payload} against the CURRENT state of its owning Parent/Student/Admin row
     * - true only if the row still exists AND is active (Parent only) AND its {@code
     * tokenVersion} still matches the token's {@code tv} claim. See this class's javadoc
     * ("tokenVersion re-check") for why this happens on every request instead of trusting the JWT
     * signature/expiry alone, and ("RAM cache") for why this reads {@link #tokenVersionCache}
     * first instead of always hitting the DB.
     */
    private boolean currentTokenVersionMatches(JwtUtil.Payload payload) {
        TokenVersionCache.CachedAccountState cached = tokenVersionCache.get(payload.role(), payload.userId());
        if (cached == null) {
            cached = loadAndCacheAccountState(payload.role(), payload.userId());
            if (cached == null) {
                return false;
            }
        }
        return cached.active() && cached.tokenVersion() == payload.tokenVersion();
    }

    /** Cache-miss path: reads the owning Parent/Student/Admin row, caches it if found, and returns it (or null if the row no longer exists - never cached, see {@link TokenVersionCache}'s javadoc). */
    private TokenVersionCache.CachedAccountState loadAndCacheAccountState(Role role, Long userId) {
        TokenVersionCache.CachedAccountState state;
        if (role == Role.PARENT) {
            Parent parent = parentRepository.findById(userId);
            if (parent == null) {
                return null;
            }
            // isActive() cached too (2026-09-04, Admin feature) - a Parent an Admin just
            // deactivated must be rejected on their very next request, not just at their next
            // login; see entity/Parent.java#active's javadoc for why no separate per-request
            // check is needed for their Students (AdminParentService#setActive(false) already
            // bumps every Student's own tokenVersion in the same call).
            state = new TokenVersionCache.CachedAccountState(parent.getTokenVersion(), parent.isActive());
        } else if (role == Role.STUDENT) {
            Student student = studentRepository.findById(userId);
            if (student == null) {
                return null;
            }
            state = new TokenVersionCache.CachedAccountState(student.getTokenVersion(), true);
        } else {
            Admin admin = adminRepository.findById(userId);
            if (admin == null) {
                return null;
            }
            state = new TokenVersionCache.CachedAccountState(admin.getTokenVersion(), true);
        }
        tokenVersionCache.put(role, userId, state);
        return state;
    }

    /** Which role a URI requires, from its {@code /api/parent/}/{@code /api/student/} prefix - null if neither (should not happen, see caller). */
    private static Role requiredRoleFor(String uri) {
        if (uri.startsWith(PARENT_PREFIX)) {
            return Role.PARENT;
        }
        if (uri.startsWith(STUDENT_PREFIX)) {
            return Role.STUDENT;
        }
        if (uri.startsWith(ADMIN_PREFIX)) {
            return Role.ADMIN;
        }
        return null;
    }

    /** The bearer token from the {@code Authorization} header, or null if absent/malformed/empty. */
    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /** Short-circuits the request with the standard {@link ApiResponse} error envelope - the filter-level equivalent of {@code BaseCtl.fail(...)}. */
    private static void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(ApiResponse.error(errorCode, message)));
    }
}
