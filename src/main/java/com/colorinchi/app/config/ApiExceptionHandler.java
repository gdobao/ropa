package com.colorinchi.app.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Handles REST API errors and returns {@link ProblemDetail} (RFC 7807) responses.
 *
 * <p>This advice targets only controllers annotated with {@link RestController},
 * so the existing {@link GlobalExceptionHandler} continues to handle Thymeleaf
 * page errors via {@code ModelAndView}.
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(WebClientResponseException.class)
    public ProblemDetail handleWebClientResponse(WebClientResponseException ex) {
        log.error("AI provider returned error: status={}, errorType={}",
                ex.getStatusCode(), ex.getClass().getSimpleName(), ex);

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "El proveedor de IA respondió con un error. Inténtalo de nuevo más tarde.");
        detail.setTitle("Bad Gateway");
        detail.setType(URI.create("https://httpstatus.io/502"));
        detail.setProperty("upstreamStatus", ex.getStatusCode().value());
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("Malformed JSON request body", ex);

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido. Verifica que el JSON esté bien formado.");
        detail.setTitle("Bad Request");
        detail.setType(URI.create("https://httpstatus.io/400"));
        return detail;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        log.error("Missing required request parameter: {}", ex.getParameterName(), ex);

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio: " + ex.getParameterName());
        detail.setTitle("Bad Request");
        detail.setType(URI.create("https://httpstatus.io/400"));
        return detail;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage());
        detail.setTitle("Too Many Requests");
        detail.setType(URI.create("https://httpstatus.io/429"));
        return detail;
    }

    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurity(SecurityException ex) {
        log.warn("Rejected REST request: {}", ex.getClass().getSimpleName());

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "La operación no está permitida.");
        detail.setTitle("Forbidden");
        detail.setType(URI.create("https://httpstatus.io/403"));
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage(), ex);

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        detail.setTitle("Bad Request");
        detail.setType(URI.create("https://httpstatus.io/400"));
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception in REST endpoint", ex);

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Inténtalo de nuevo.");
        detail.setTitle("Internal Server Error");
        detail.setType(URI.create("https://httpstatus.io/500"));
        return detail;
    }
}
