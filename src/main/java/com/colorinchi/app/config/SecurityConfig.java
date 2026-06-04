package com.colorinchi.app.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminProperties adminProperties;

    public SecurityConfig(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**", "/admin/**")
                    .access((authentication, context) -> adminDecision(context))
                .anyRequest().permitAll()
            )
            .headers(headers -> {
                headers.frameOptions(frame -> frame
                    .deny()
                );
                // style-src keeps 'unsafe-inline' because CSP nonces only apply to <style>
                // elements, not style="" attributes. The remaining 12 th:style usages set
                // server-computed colors and widths for colorimetry UI. Inline styles
                // cannot execute code and values are server-generated, not user-controlled.
                // Pinned to HTMX 2.0.4 sha256 hash (no broad CDN allowlist).
                // The SRI integrity attribute on the <script> tag is the primary integrity control;
                // the CSP hash acts as a second layer and prevents any other unpkg script from running.
                headers.contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self' 'sha256-4gndpcgjVHnzFm3vx3UOHbzVpcGAi3eS/C5nM3aPtEc='; img-src 'self' data: blob: https://placehold.co")
                );
                headers.referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                );
                headers.permissionsPolicyHeader(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=()")
                );
            })
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    private AuthorizationDecision adminDecision(RequestAuthorizationContext context) {
        if (!adminProperties.enabled()) {
            return new AuthorizationDecision(false);
        }

        String provided = context.getRequest().getHeader("X-Admin-Token");
        if (provided == null || provided.isBlank()) {
            return new AuthorizationDecision(false);
        }

        byte[] expected = adminProperties.token().getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided.getBytes(StandardCharsets.UTF_8);
        return new AuthorizationDecision(MessageDigest.isEqual(expected, actual));
    }
}
