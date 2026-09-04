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
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.AdminRepository;
import vn.org.thn.service.base.exception.CommonErrorCode;
import vn.org.thn.service.base.exception.ErrorCode;
import vn.org.thn.service.base.response.ApiResponse;
import vn.org.thn.service.base.util.JsonUtils;

import java.io.IOException;
import java.util.Set;

/**
 * Closes the write-side security gap on {@code base}'s {@code LanguageApi} (2026-09-04, part 4/4
 * of the "Admin manages translations" feature): that API ({@code /public/language/**}) ships
 * inside {@code base} itself, is auto-configured (see {@code LanguageAutoConfiguration}) and has
 * NO authentication of its own by design - {@code base} has no auth/security layer at all (see
 * {@code claude/base-module-status.md}) - so every one of its endpoints, including the
 * add/update/delete ones, is reachable by anyone who can reach quiz-service at all.
 * <p>
 * This filter is quiz-service-only code (not a change to {@code base}, same "don't touch base/"
 * discipline as everywhere else in this app - see {@code QuizRequestBase.ts}'s comment for the
 * frontend equivalent of this rule). It is registered (see {@code config/SecurityConfig}) against
 * {@code /public/language} and {@code /public/language/*} ONLY - every other {@code /public/**}
 * path {@code base} may ship stays exactly as unauthenticated as before.
 * <p>
 * <b>Read vs write split:</b> {@code GET}/{@code HEAD}/{@code OPTIONS} pass through untouched -
 * the Frontend's runtime translation-overlay fetch ({@code GET /public/language/{lang}}, wired in
 * {@code IBlocUI.loadLang}/{@code BlocApplication.loadInit}) and the Admin table's list load
 * ({@code GET /public/language/list}) both run BEFORE login (or without any token at all for a
 * Student mid-test), so they must stay open exactly like the static {@code vi.json}/{@code
 * en.json} they overlay. Every other method - {@code POST}/{@code PUT}/{@code DELETE}, which
 * covers {@code addOrUpdate}, both delete endpoints, AND {@code /export} (a POST in {@code
 * LanguageApi} despite being read-only in effect, exposes every translation row at once as a
 * download) - now requires a validly-signed, unexpired, tokenVersion-matching ADMIN token, same
 * verification steps as {@link JwtAuthFilter} (deliberately duplicated rather than reused - see
 * this class's sibling javadoc note in {@code SecurityConfig} for why a shared filter was not
 * used instead).
 */
public class PublicLanguageAdminGuardFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PublicLanguageAdminGuardFilter.class);

    private static final Set<String> OPEN_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final AdminRepository adminRepository;
    private final TokenVersionCache tokenVersionCache;

    public PublicLanguageAdminGuardFilter(JwtUtil jwtUtil, AdminRepository adminRepository, TokenVersionCache tokenVersionCache) {
        this.jwtUtil = jwtUtil;
        this.adminRepository = adminRepository;
        this.tokenVersionCache = tokenVersionCache;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (OPEN_METHODS.contains(httpRequest.getMethod())) {
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
            log.warn("Rejected invalid token on {} {}: {}", httpRequest.getMethod(), httpRequest.getRequestURI(), e.getMessage());
            writeError(httpResponse, QuizErrorCode.UNAUTHORIZED, "Invalid token");
            return;
        }

        if (payload.role() != Role.ADMIN) {
            writeError(httpResponse, CommonErrorCode.FORBIDDEN,
                    "Token role " + payload.role() + " cannot modify translations");
            return;
        }

        // Goes through TokenVersionCache first (2026-09-04, same RAM-cache mechanism as
        // JwtAuthFilter - see that class's "RAM cache" javadoc and TokenVersionCache's own
        // javadoc for the full design) instead of always hitting the DB; a cache miss falls
        // through to a repository read and populates the cache for next time.
        TokenVersionCache.CachedAccountState cached = tokenVersionCache.get(Role.ADMIN, payload.userId());
        if (cached == null) {
            Admin admin = adminRepository.findById(payload.userId());
            if (admin == null) {
                writeError(httpResponse, QuizErrorCode.UNAUTHORIZED,
                        "Token no longer valid - the account was removed, or logged out from all devices");
                return;
            }
            cached = new TokenVersionCache.CachedAccountState(admin.getTokenVersion(), true);
            tokenVersionCache.put(Role.ADMIN, payload.userId(), cached);
        }
        if (cached.tokenVersion() != payload.tokenVersion()) {
            writeError(httpResponse, QuizErrorCode.UNAUTHORIZED,
                    "Token no longer valid - the account was removed, or logged out from all devices");
            return;
        }

        chain.doFilter(request, response);
    }

    /** The bearer token from the {@code Authorization} header, or null if absent/malformed/empty - identical to {@link JwtAuthFilter}'s helper of the same name, duplicated rather than shared (see this class's javadoc). */
    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /** Short-circuits the request with the standard {@link ApiResponse} error envelope - identical to {@link JwtAuthFilter}'s helper of the same name. */
    private static void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(ApiResponse.error(errorCode, message)));
    }
}
