package com.colorinchi.app.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Builds the JSON request body for the provider's chat completions API
 * (OpenAI-compatible format).
 */
@Component
public class ProviderRequestFactory {

    /**
     * Create a streaming chat completion request body.
     *
     * @param model     the provider API model name
     * @param messages  list of message maps, each with {@code role} and
     *                  {@code content} keys
     * @param maxTokens maximum tokens for the response
     * @return a mutable-free map suitable for serialization
     */
    public Map<String, Object> createChatRequest(
            String model,
            List<Map<String, String>> messages,
            int maxTokens) {

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);
        body.put("messages", messages);
        return Map.copyOf(body);
    }
}
