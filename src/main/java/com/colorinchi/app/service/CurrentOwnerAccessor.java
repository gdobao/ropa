package com.colorinchi.app.service;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentOwnerAccessor {

    public static final String CURRENT_OWNER_ID_ATTRIBUTE = CurrentOwnerAccessor.class.getName() + ".CURRENT_OWNER_ID";

    public UUID getCurrentOwnerId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("Owner context unavailable");
        }

        HttpServletRequest request = attributes.getRequest();
        Object ownerId = request.getAttribute(CURRENT_OWNER_ID_ATTRIBUTE);
        if (ownerId instanceof UUID uuid) {
            return uuid;
        }

        throw new IllegalStateException("Owner context unavailable");
    }
}
