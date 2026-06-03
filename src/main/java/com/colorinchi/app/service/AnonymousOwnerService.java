package com.colorinchi.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
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

    static final String OWNER_COOKIE_NAME = "owner_token";
    private static final Duration OWNER_COOKIE_MAX_AGE = Duration.ofDays(365);
    private static final int TOKEN_BYTES = 32;

    private final AnonymousOwnerRepository anonymousOwnerRepository;
    private final SecureRandom secureRandom;

    public AnonymousOwnerService(AnonymousOwnerRepository anonymousOwnerRepository) {
        this.anonymousOwnerRepository = anonymousOwnerRepository;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public UUID resolveOwnerId(HttpServletRequest request, HttpServletResponse response) {
        String token = readOwnerToken(request.getCookies());
        if (token != null) {
            String tokenHash = tokenHash(token);
            var owner = anonymousOwnerRepository.findByTokenHash(tokenHash);
            if (owner.isPresent()) {
                return owner.get().getId();
            }
        }

        String newToken = generateToken();
        AnonymousOwner owner;
        try {
            owner = anonymousOwnerRepository.findFirstByBootstrapTrueOrderByCreatedAtAsc()
                    .filter(AnonymousOwner::isBootstrap)
                    .map(bootstrapOwner -> claimBootstrapOwner(bootstrapOwner, newToken))
                    .orElseGet(() -> createAnonymousOwner(newToken));
        } catch (Exception e) {
            owner = createAnonymousOwner(newToken);
        }

        writeOwnerCookie(response, newToken, isSecureRequest(request));
        return owner.getId();
    }

    private String readOwnerToken(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> OWNER_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private AnonymousOwner claimBootstrapOwner(AnonymousOwner owner, String token) {
        owner.setBootstrap(false);
        owner.setClaimedAt(OffsetDateTime.now());
        owner.setTokenHash(tokenHash(token));
        return anonymousOwnerRepository.save(owner);
    }

    private AnonymousOwner createAnonymousOwner(String token) {
        AnonymousOwner owner = new AnonymousOwner();
        owner.setId(UUID.randomUUID());
        owner.setBootstrap(false);
        owner.setClaimedAt(OffsetDateTime.now());
        owner.setTokenHash(tokenHash(token));
        return anonymousOwnerRepository.save(owner);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String tokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for owner token hashing", ex);
        }
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private void writeOwnerCookie(HttpServletResponse response, String token, boolean secureRequest) {
        String cookie = ResponseCookie.from(OWNER_COOKIE_NAME, token)
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
