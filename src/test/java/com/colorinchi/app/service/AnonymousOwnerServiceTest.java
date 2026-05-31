package com.colorinchi.app.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.repository.AnonymousOwnerRepository;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymousOwnerServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private AnonymousOwnerRepository anonymousOwnerRepository;

    @InjectMocks
    private AnonymousOwnerService service;

    @Test
    void resolveOwnerIdReusesExistingValidCookieWithoutReissuingCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("owner_token", "known-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AnonymousOwner owner = owner(OWNER_ID, false);

        when(anonymousOwnerRepository.findByTokenHash(anyString())).thenReturn(Optional.of(owner));

        UUID resolved = service.resolveOwnerId(request, response);

        assertThat(resolved).isEqualTo(OWNER_ID);
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        verify(anonymousOwnerRepository, never()).save(any());
    }

    @Test
    void resolveOwnerIdIssuesHttpOnlyLaxCookieWithoutSecureOnHttp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AnonymousOwner bootstrapOwner = owner(OWNER_ID, true);

        when(anonymousOwnerRepository.findFirstByBootstrapTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.of(bootstrapOwner));
        when(anonymousOwnerRepository.save(any(AnonymousOwner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID resolved = service.resolveOwnerId(request, response);

        assertThat(resolved).isEqualTo(OWNER_ID);
        assertThat(response.getHeader("Set-Cookie"))
                .contains("owner_token=")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Path=/")
                .doesNotContain("Secure");
        assertThat(bootstrapOwner.getTokenHash()).isNotBlank();
    }

    @Test
    void resolveOwnerIdIssuesSecureCookieOnHttps() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AnonymousOwner bootstrapOwner = owner(OWNER_ID, true);

        when(anonymousOwnerRepository.findFirstByBootstrapTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.of(bootstrapOwner));
        when(anonymousOwnerRepository.save(any(AnonymousOwner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveOwnerId(request, response);

        assertThat(response.getHeader("Set-Cookie")).contains("Secure");
    }

    @Test
    void resolveOwnerIdIssuesSecureCookieBehindHttpsProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AnonymousOwner bootstrapOwner = owner(OWNER_ID, true);

        when(anonymousOwnerRepository.findFirstByBootstrapTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.of(bootstrapOwner));
        when(anonymousOwnerRepository.save(any(AnonymousOwner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveOwnerId(request, response);

        assertThat(response.getHeader("Set-Cookie")).contains("Secure");
    }

    @Test
    void resolveOwnerIdIgnoresLegacyUnsignedOwnerIdCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("owner_id", OWNER_ID.toString()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AnonymousOwner bootstrapOwner = owner(OWNER_ID, true);

        when(anonymousOwnerRepository.findFirstByBootstrapTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.of(bootstrapOwner));
        when(anonymousOwnerRepository.save(any(AnonymousOwner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveOwnerId(request, response);

        assertThat(response.getHeader("Set-Cookie")).contains("owner_token=");
    }

    private AnonymousOwner owner(UUID ownerId, boolean bootstrap) {
        AnonymousOwner owner = new AnonymousOwner();
        owner.setId(ownerId);
        owner.setBootstrap(bootstrap);
        return owner;
    }
}
