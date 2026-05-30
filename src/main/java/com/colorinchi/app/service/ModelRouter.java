package com.colorinchi.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.config.AiServerProperties;

/**
 * Picks the correct model from the user's request, validates it is in the
 * allowlist, and resolves to the actual provider configuration.
 *
 * Falls back to a single-entry catalog from the legacy {@code app.ai.model}
 * field when the new models catalog is not configured.
 */
@Service
public class ModelRouter {

    private final AiServerProperties properties;
    private final List<AiModelConfig> catalog;

    public ModelRouter(AiServerProperties properties) {
        this.properties = properties;
        if (properties.models() != null && !properties.models().isEmpty()) {
            this.catalog = List.copyOf(properties.models());
        } else {
            // Backward compatibility: build single-entry catalog from legacy config
            AiModelConfig legacy = new AiModelConfig();
            legacy.setId(properties.model());
            legacy.setName(properties.model());
            legacy.setDefault(true);
            this.catalog = List.of(legacy);
        }
    }

    /**
     * Resolve a requested model ID to its {@link AiModelConfig}.
     *
     * @param requestedModelId the model ID from the user's request, or
     *                         {@code null}/{@code empty} to use the default
     * @return the resolved model config
     * @throws IllegalArgumentException if the model is not in the allowlist
     */
    public AiModelConfig resolve(String requestedModelId) {
        if (!StringUtils.hasText(requestedModelId)) {
            return findDefault();
        }
        return catalog.stream()
                .filter(m -> m.getId().equals(requestedModelId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Modelo no soportado: " + requestedModelId
                                + ". Modelos disponibles: " + availableModelIds()));
    }

    /**
     * Return the default model (first marked {@code default}, or the first
     * entry in the catalog).
     */
    public AiModelConfig findDefault() {
        return catalog.stream()
                .filter(AiModelConfig::isDefault)
                .findFirst()
                .orElse(catalog.getFirst());
    }

    /** Return an unmodifiable view of the full model catalog. */
    public List<AiModelConfig> getAvailableModels() {
        return catalog;
    }

    private String availableModelIds() {
        return catalog.stream()
                .map(AiModelConfig::getId)
                .collect(Collectors.joining(", "));
    }
}
