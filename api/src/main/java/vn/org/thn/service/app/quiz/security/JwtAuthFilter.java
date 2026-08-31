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
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.base.exception.CommonErrorCode;
import vn.org.thn.service.base.exception.ErrorCode;
import vn.org.thn.service.base.response.ApiResponse;
import vn.org.thn.service.base.util.JsonUtils;

import java.io.IOException;

/**
 * Verifies the {@code Authorization: Bearer <token>} header on every request under {@code
 * /api/parent/*}/{@code /api/student/*} (see registration + URL patterns in {@code
 * config/SecurityConfig}), modeled on {@code base}'s {@code RequestContextFilter} - a plain
 * {@link Filter}, not full Spring Security, kept intentionally simple to match the rest of {@code
 * base}. {@code /api/auth/**} is never matched by this filter's URL patterns, so the 3 register/
 * login endpoints stay open.
 * <p>
 * On success, stores the resolved {@link CurrentUser} as a request attribute for {@link
 * CurrentUser#get()} to read back in the Service layer. On failure, writes the standard {@link
 * ApiResponse} error envelope directly (this runs before the DispatcherServlet, so {@code
 * GlobalExceptionHandler} never sees a rejection here - it only handles exceptions thrown from
 * inside a controller method).
 */
public class JwtAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String PARENT_PREFIX = "/api/parent/";
    private static final String STUDENT_PREFIX = "/api/student/";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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

        httpRequest.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, new CurrentUser(payload.userId(), payload.role()));
        chain.doFilter(request, response);
    }

    /** Which role a URI requires, from its {@code /api/parent/}/{@code /api/student/} prefix - null if neither (should not happen, see caller). */
    private static Role requiredRoleFor(String uri) {
        if (uri.startsWith(PARENT_PREFIX)) {
            return Role.PARENT;
        }
        if (uri.startsWith(STUDENT_PREFIX)) {
            return Role.STUDENT;
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
