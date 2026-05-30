package com.colorinchi.app.service;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.colorinchi.app.dto.chat.PolicyDecision;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatPolicyServiceTest {

    @Mock
    private ChatIntentClassifier intentClassifier;

    @Mock
    private CurrentOwnerAccessor currentOwnerAccessor;

    @Mock
    private ChatAnalyticsService chatAnalyticsService;

    @Mock
    private ChatMetricsService chatMetricsService;

    private ChatPolicyService policyService;

    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        policyService = new ChatPolicyService(intentClassifier, currentOwnerAccessor, chatAnalyticsService, chatMetricsService);
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
    }

    @Test
    void allowForGeneralChat() {
        when(intentClassifier.classify("hola")).thenReturn(ChatIntentClassifier.Intent.GENERAL_CHAT);

        PolicyDecision decision = policyService.evaluate("hola");

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.decision()).isEqualTo(PolicyDecision.Decision.ALLOW);
    }

    @Test
    void flagForStylingAdvice() {
        when(intentClassifier.classify("cómo combino esta camisa"))
                .thenReturn(ChatIntentClassifier.Intent.STYLING_ADVICE);

        PolicyDecision decision = policyService.evaluate("cómo combino esta camisa");

        assertThat(decision.decision()).isEqualTo(PolicyDecision.Decision.FLAG);
    }

    @Test
    void flagForColorAdvice() {
        when(intentClassifier.classify("qué colores combinan con azul"))
                .thenReturn(ChatIntentClassifier.Intent.COLOR_ADVICE);

        PolicyDecision decision = policyService.evaluate("qué colores combinan con azul");

        assertThat(decision.decision()).isEqualTo(PolicyDecision.Decision.FLAG);
    }

    @Test
    void blockForOutfitRequest() {
        when(intentClassifier.classify("pick an outfit for me"))
                .thenReturn(ChatIntentClassifier.Intent.OUTFIT_REQUEST);

        PolicyDecision decision = policyService.evaluate("pick an outfit for me");

        assertThat(decision.decision()).isEqualTo(PolicyDecision.Decision.BLOCK);
        assertThat(decision.reason()).contains("outfit_request");
        assertThat(decision.refusalMessage()).isNotBlank();
    }

    @Test
    void blockForOutfitRequestInSpanish() {
        when(intentClassifier.classify("armame un look"))
                .thenReturn(ChatIntentClassifier.Intent.OUTFIT_REQUEST);

        PolicyDecision decision = policyService.evaluate("armame un look");

        assertThat(decision.decision()).isEqualTo(PolicyDecision.Decision.BLOCK);
        assertThat(decision.reason()).contains("outfit_request");
        assertThat(decision.refusalMessage()).isNotBlank();
    }

    @Test
    void rateLimitBlocksAfterThreshold() {
        when(intentClassifier.classify(anyString())).thenReturn(ChatIntentClassifier.Intent.GENERAL_CHAT);

        // Send many messages quickly (rate limit is 30 per minute, but we can hit it)
        PolicyDecision lastDecision = null;
        for (int i = 0; i < 35; i++) {
            lastDecision = policyService.evaluate("hola " + i);
        }

        // At least one should be blocked or flagged
        assertThat(lastDecision.decision()).isIn(
                PolicyDecision.Decision.BLOCK, PolicyDecision.Decision.FLAG);
    }

    @Test
    void evaluateUsesCurrentOwnerForRateLimit() {
        when(intentClassifier.classify("hola")).thenReturn(ChatIntentClassifier.Intent.GENERAL_CHAT);

        PolicyDecision decision = policyService.evaluate("hola");

        assertThat(decision.isAllowed()).isTrue();
    }

    @Test
    void checkRateLimitReturnsAllowForFirstRequest() {
        PolicyDecision decision = policyService.checkRateLimit(ownerId);

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.decision()).isEqualTo(PolicyDecision.Decision.ALLOW);
    }
}
