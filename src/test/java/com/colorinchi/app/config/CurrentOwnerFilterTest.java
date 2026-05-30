package com.colorinchi.app.config;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.colorinchi.app.service.AnonymousOwnerService;
import com.colorinchi.app.service.CurrentOwnerAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentOwnerFilterTest {

    @Mock
    private AnonymousOwnerService anonymousOwnerService;

    @Test
    void filterStoresResolvedOwnerOnRequest() throws Exception {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        CurrentOwnerFilter filter = new CurrentOwnerFilter(anonymousOwnerService);

        when(anonymousOwnerService.resolveOwnerId(request, response)).thenReturn(ownerId);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(CurrentOwnerAccessor.CURRENT_OWNER_ID_ATTRIBUTE)).isEqualTo(ownerId);
    }
}
