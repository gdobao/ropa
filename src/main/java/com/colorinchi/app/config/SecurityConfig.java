package com.colorinchi.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**")
                    .access((authentication, context) -> {
                        var request = context.getRequest();
                        String remoteAddr = request.getRemoteAddr();
                        if ("127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr)
                                || "0:0:0:0:0:0:0:1".equals(remoteAddr)) {
                            return new org.springframework.security.authorization.AuthorizationDecision(true);
                        }
                        return new org.springframework.security.authorization.AuthorizationDecision(false);
                    })
                .anyRequest().permitAll()
            )
            .headers(headers -> {
                headers.frameOptions(frame -> frame
                    .deny()
                );
                headers.contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self' 'unsafe-inline' https://unpkg.com; img-src 'self' data: blob: https://placehold.co")
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
}
