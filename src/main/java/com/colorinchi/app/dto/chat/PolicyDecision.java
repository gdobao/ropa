package com.colorinchi.app.dto.chat;

public record PolicyDecision(
    Decision decision,
    String reason,
    String refusalMessage
) {
    public enum Decision {
        ALLOW,
        BLOCK,
        FLAG
    }

    public static PolicyDecision allow(String reason) {
        return new PolicyDecision(Decision.ALLOW, reason, null);
    }

    public static PolicyDecision block(String reason, String refusalMessage) {
        return new PolicyDecision(Decision.BLOCK, reason, refusalMessage);
    }

    public static PolicyDecision flag(String reason) {
        return new PolicyDecision(Decision.FLAG, reason, null);
    }

    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }
}
