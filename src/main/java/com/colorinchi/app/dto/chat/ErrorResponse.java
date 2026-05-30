package com.colorinchi.app.dto.chat;

public record ErrorResponse(
    String error,
    String message
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message);
    }
}
