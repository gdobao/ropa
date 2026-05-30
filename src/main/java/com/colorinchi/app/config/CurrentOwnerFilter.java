package com.colorinchi.app.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.colorinchi.app.service.AnonymousOwnerService;
import com.colorinchi.app.service.CurrentOwnerAccessor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CurrentOwnerFilter extends OncePerRequestFilter {

    private final AnonymousOwnerService anonymousOwnerService;

    public CurrentOwnerFilter(AnonymousOwnerService anonymousOwnerService) {
        this.anonymousOwnerService = anonymousOwnerService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        UUID ownerId = anonymousOwnerService.resolveOwnerId(request, response);
        request.setAttribute(
                CurrentOwnerAccessor.CURRENT_OWNER_ID_ATTRIBUTE,
                ownerId);
        try {
            MDC.put("ownerId", ownerId.toString());
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
