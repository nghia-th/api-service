package vn.org.thn.service.app.quiz.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.security.JwtUtil;

/**
 * Registers {@link JwtAuthFilter} against {@code /api/parent/*} and {@code /api/student/*} only -
 * {@code /api/auth/**} is never matched by these patterns, so register/login stay open. Order 2,
 * i.e. after {@code base}'s {@code RequestContextFilter} (order 1, registered in {@code
 * BaseWebAutoConfiguration}), so MDC/request-id/client-ip are already populated when this filter
 * runs and any 401/403 it logs carries them too.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtUtil jwtUtil) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(new JwtAuthFilter(jwtUtil));
        registration.setOrder(2);
        registration.addUrlPatterns("/api/parent/*", "/api/student/*");
        return registration;
    }
}
