package vn.org.thn.service.app.quiz.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.org.thn.service.app.quiz.repository.AdminRepository;
import vn.org.thn.service.app.quiz.repository.ParentRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.security.JwtUtil;

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
 */
@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(
            JwtUtil jwtUtil, ParentRepository parentRepository, StudentRepository studentRepository,
            AdminRepository adminRepository) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(
                new JwtAuthFilter(jwtUtil, parentRepository, studentRepository, adminRepository));
        registration.setOrder(2);
        registration.addUrlPatterns("/api/parent/*", "/api/student/*", "/api/admin/*");
        return registration;
    }
}
