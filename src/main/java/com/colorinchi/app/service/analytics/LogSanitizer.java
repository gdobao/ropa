package com.colorinchi.app.service.analytics;

import java.util.regex.Pattern;

/**
 * Utility that redacts sensitive data from log lines.
 *
 * <p>Applied at key logging points in chat services — it is NOT a blanket
 * logging filter. Each call site explicitly invokes {@link #sanitize(String)}.
 *
 * <h3>Redaction rules</h3>
 * <ul>
 *   <li>Owner identifiers (ownerId, owner_id, cookie values)</li>
 *   <li>Raw message content and AI prompt text</li>
 *   <li>Wardrobe context data</li>
 * </ul>
 *
 * <h3>Preserved fields</h3>
 * <ul>
 *   <li>Event types, model names, latency, token counts</li>
 *   <li>Error types (not messages), run IDs, session IDs</li>
 * </ul>
 */
public final class LogSanitizer {

    private static final String REDACTED = "[REDACTED]";

    // UUID pattern
    private static final Pattern UUID_PATTERN =
            Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

    // Redact ownerId=<uuid> or owner_id=<uuid> or owner=<uuid>
    private static final Pattern OWNER_PATTERN =
            Pattern.compile("(owner[Ii]d|owner_id|owner|CURRENT_OWNER_ID)([=:]\\s*)(" + UUID_PATTERN.pattern() + ")");

    // Redact content='...', content="...", message='...', message="...", prompt='...', prompt="..."
    // Uses a negative lookahead relative to the captured quote to allow the
    // other quote character inside the content (e.g. JSON within single quotes).
    private static final Pattern CONTENT_PATTERN =
            Pattern.compile(
                    "(?i)(content|message|prompt|userMessage|botMessage|wardrobeContext|wardrobe_context)"
                            + "([=:]\\s*)(['\"])"
                            + "((?!\\3).{0,200}?)"
                            + "\\3");

    // Redact cookie-like values: owner_id=..., preserving the delimiter
    private static final Pattern COOKIE_PATTERN =
            Pattern.compile("(?i)(owner_id|anonymousOwnerId|anon_owner)([=:])[^\";,}\\s]{8,}");

    private LogSanitizer() {
        // utility
    }

    /**
     * Sanitize a log line by replacing sensitive values with {@code [REDACTED]}.
     *
     * @param logLine the raw log line (nullable)
     * @return sanitized line, or null if input was null
     */
    public static String sanitize(String logLine) {
        if (logLine == null || logLine.isEmpty()) {
            return logLine;
        }

        String result = logLine;

        // Order matters: more specific patterns first
        result = OWNER_PATTERN.matcher(result).replaceAll("$1$2" + REDACTED);
        result = CONTENT_PATTERN.matcher(result).replaceAll("$1$2" + REDACTED);
        result = COOKIE_PATTERN.matcher(result).replaceAll("$1$2" + REDACTED);

        return result;
    }
}
