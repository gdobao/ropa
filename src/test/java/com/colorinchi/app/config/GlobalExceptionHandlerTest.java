package com.colorinchi.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesIllegalArgumentException() {
        ModelAndView mav = handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getModel()).containsEntry("errorTitle", "Solicitud inválida");
        assertThat(mav.getModel()).containsEntry("errorMessage", "bad input");
    }

    @Test
    void handlesSecurityException() {
        ModelAndView mav = handler.handleSecurity(new SecurityException("forbidden"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getModel()).containsEntry("errorTitle", "Acceso denegado");
        assertThat(mav.getModel()).containsEntry("errorMessage", "forbidden");
    }

    @Test
    void handlesRateLimitExceededException() {
        ModelAndView mav = handler.handleRateLimit(new RateLimitExceededException("too many"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getModel()).containsEntry("errorTitle", "Demasiadas solicitudes");
        assertThat(mav.getModel()).containsEntry("errorMessage", "too many");
    }

    @Test
    void handlesGenericException() {
        ModelAndView mav = handler.handleGeneric(new RuntimeException("unexpected"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getModel()).containsEntry("errorTitle", "Error interno");
        assertThat(mav.getModel()).containsEntry("errorMessage", "Ocurrió un error inesperado. Intenta de nuevo.");
    }
}
