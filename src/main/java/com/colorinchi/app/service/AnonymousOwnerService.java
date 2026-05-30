package com.colorinchi.app.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.repository.AnonymousOwnerRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AnonymousOwnerService {

    static final String OWNER_COOKIE_NAME = "owner_id";
    private static final Duration OWNER_COOKIE_MAX_AGE = Duration.ofDays(365);

    private final AnonymousOwnerRepository anonymousOwnerRepository;

    public AnonymousOwnerService(AnonymousOwnerRepository anonymousOwnerRepository) {
        this.anonymousOwnerRepository = anonymousOwnerRepository;
    }

    @Transactional
    public UUID resolveOwnerId(HttpServletRequest request, HttpServletResponse response) {
        UUID cookieOwnerId = readOwnerId(request.getCookies());
        if (cookieOwnerId != null && anonymousOwnerRepository.existsById(cookieOwnerId)) {
            return cookieOwnerId;
        }

        AnonymousOwner owner = anonymousOwnerRepository.findFirstByBootstrapTrueOrderByCreatedAtAsc()
                .map(this::claimBootstrapOwner)
                .orElseGet(this::createAnonymousOwner);

        writeOwnerCookie(response, owner.getId(), request.isSecure());
        return owner.getId();
    }

    private UUID readOwnerId(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> OWNER_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(Objects::nonNull)
                .map(this::parseUuid)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private UUID parseUuid(String rawValue) {
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private AnonymousOwner claimBootstrapOwner(AnonymousOwner owner) {
        owner.setBootstrap(false);
        owner.setClaimedAt(OffsetDateTime.now());
        return anonymousOwnerRepository.save(owner);
    }

    private AnonymousOwner createAnonymousOwner() {
        AnonymousOwner owner = new AnonymousOwner();
        owner.setId(UUID.randomUUID());
        owner.setBootstrap(false);
        owner.setClaimedAt(OffsetDateTime.now());
        return anonymousOwnerRepository.save(owner);
    }

    private void writeOwnerCookie(HttpServletResponse response, UUID ownerId, boolean secureRequest) {
        String cookie = ResponseCookie.from(OWNER_COOKIE_NAME, ownerId.toString())
                .httpOnly(true)
                .secure(secureRequest)
                .sameSite("Lax")
                .path("/")
                .maxAge(OWNER_COOKIE_MAX_AGE)
                .build()
                .toString();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie);
    }
}
