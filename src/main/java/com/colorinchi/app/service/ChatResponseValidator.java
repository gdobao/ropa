package com.colorinchi.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.colorinchi.app.dto.chat.ValidationResult;

@Component
public class ChatResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(ChatResponseValidator.class);

    /**
     * Patterns that indicate the AI is being overly prescriptive about outfit choices.
     */
    private static final List<Pattern> PRESCRIPTIVE_PATTERNS = List.of(
        Pattern.compile("te recomiendo (este|este outfit|que uses|que te pongas|que combines)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(ponete|usá|vestite con|elegí este|llevalo puesto)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^(te sugiero|te recomiendo|mi recomendación es|lo mejor es que uses)\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Patterns that match expected refusal / graceful boundaries.
     */
    private static final List<Pattern> REFUSAL_PATTERNS = List.of(
        Pattern.compile("no (puedo|debo) elegir", Pattern.CASE_INSENSITIVE),
        Pattern.compile("la decisión final (es tuya|te pertenece)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("mi rol es (aconsejar|informar|ayudar|guiar)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("no (es|sería) apropiado que (yo|te) (elija|decida)", Pattern.CASE_INSENSITIVE)
    );

    public ValidationResult validate(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return ValidationResult.invalid("Respuesta vacía del asistente");
        }

        List<String> warnings = new ArrayList<>();

        // Check for overly prescriptive language
        for (Pattern p : PRESCRIPTIVE_PATTERNS) {
            if (p.matcher(aiResponse).find()) {
                warnings.add("Respuesta potencialmente prescriptiva: '" + p.pattern() + "'");
                log.warn("Policy drift detected: prescriptive pattern matched in AI response");
            }
        }

        if (!warnings.isEmpty()) {
            return new ValidationResult(false, warnings);
        }

        return ValidationResult.valid();
    }

    /**
     * Checks whether the response contains an expected refusal pattern.
     */
    public boolean isExpectedRefusal(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return false;
        }
        return REFUSAL_PATTERNS.stream().anyMatch(p -> p.matcher(aiResponse).find());
    }
}
