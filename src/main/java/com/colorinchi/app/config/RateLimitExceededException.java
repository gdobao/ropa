package com.colorinchi.app.config;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super();
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}
