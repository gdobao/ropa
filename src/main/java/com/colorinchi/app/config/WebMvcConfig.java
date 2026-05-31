package com.colorinchi.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    public WebMvcConfig(UploadProperties uploadProperties, RateLimitingInterceptor rateLimitingInterceptor) {
        this.uploadProperties = uploadProperties;
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadProperties.directory().toAbsolutePath().normalize().toUri().toString());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns(
                        "/wardrobe/analyze",
                        "/recommendation",
                        "/api/chat/stream/**",
                        "/api/chat/sessions",
                        "/api/chat/sessions/**",
                        "/api/chat/messages/**",
                        "/api/companion/messages/**",
                        "/api/companion/stream/**",
                        "/api/companion/sessions",
                        "/api/companion/sessions/**");
    }
}
