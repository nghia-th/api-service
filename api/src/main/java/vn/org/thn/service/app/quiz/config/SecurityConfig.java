package vn.org.thn.service.app.quiz.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.org.thn.service.app.quiz.repository.AdminRepository;
import vn.org.thn.service.app.quiz.repository.ParentRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.security.JwtUtil;
import vn.org.thn.service.app.quiz.security.PublicLanguageAdminGuardFilter;
import vn.org.thn.service.app.quiz.security.TokenVersionCache;

/**
 * Registers {@link JwtAuthFilter} against {@code /api/parent/*}, {@code /api/student/*} and
 * (2026-09-04) {@code /api/admin/*} - {@code /api/auth/**} is never matched by these patterns, so
 * register/login/refresh/logout stay open. Order 2, i.e. after {@code base}'s {@code
 * RequestContextFilter} (order 1, registered in {@code BaseWebAutoConfiguration}), so MDC/
 * request-id/client-ip are already populated when this filter runs and any 401/403 it logs
 * carries them too.
 * <p>
 * {@code ParentRepository}/{@code StudentRepository}/{@code AdminRepository} (tokenVersion
 * re-check, see {@code JwtAuthFilter}'s javadoc) are passed straight through to {@link
 * JwtAuthFilter}'s constructor - {@code JwtAuthFilter} itself is a plain {@link
 * jakarta.servlet.Filter}, not a Spring bean, so it cannot {@code @Autowired} them itself; this
 * {@code @Bean} method's parameters are the only place Spring injection reaches it.
 * <p>
 * {@link PublicLanguageAdminGuardFilter} (2026-09-04, part 4/4) is registered separately, ONLY
 * against {@code /public/language} and {@code /public/language/*} - it is NOT folded into {@link
 * JwtAuthFilter} on purpose: that filter's {@code requiredRoleFor(uri)} is a clean, deliberately
 * method-agnostic one-role-per-prefix lookup (see its javadoc), and {@code /public/language/**}
 * needs the opposite - one path whose required role depends on the HTTP METHOD (GET/HEAD/OPTIONS
 * open, everything else ADMIN-only, see {@link PublicLanguageAdminGuardFilter}'s own javadoc) -
 * adding that one exception into {@code JwtAuthFilter} would complicate every other request it
 * guards for the sake of a single unrelated path. Also order 2 (same as {@code
 * jwtAuthFilterRegistration}) - the two filters' URL patterns never overlap ({@code
 * /api/parent/*}/{@code /api/student/*}/{@code /api/admin/*} vs {@code /public/language*}), so
 * relative order between them does not matter.
 * <p>
 * Both filters now also take a {@link TokenVersionCache} (2026-09-04, RAM cache) - see that
 * interface's javadoc for the design, and each filter's own javadoc for how it uses it.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(
            JwtUtil jwtUtil, ParentRepository parentRepository, StudentRepository studentRepository,
            AdminRepository adminRepository, TokenVersionCache tokenVersionCache) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(
                new JwtAuthFilter(jwtUtil, parentRepository, studentRepository, adminRepository, tokenVersionCache));
        registration.setOrder(2);
        registration.addUrlPatterns("/api/parent/*", "/api/student/*", "/api/admin/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<PublicLanguageAdminGuardFilter> publicLanguageAdminGuardFilterRegistration(
            JwtUtil jwtUtil, AdminRepository adminRepository, TokenVersionCache tokenVersionCache) {
        FilterRegistrationBean<PublicLanguageAdminGuardFilter> registration = new FilterRegistrationBean<>(
                new PublicLanguageAdminGuardFilter(jwtUtil, adminRepository, tokenVersionCache));
        registration.setOrder(2);
        registration.addUrlPatterns("/public/language", "/public/language/*");
        return registration;
    }
}
