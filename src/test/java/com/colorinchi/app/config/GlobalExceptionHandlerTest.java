package com.colorinchi.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesIllegalArgumentException() {
        ModelAndView mav = handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mav.getModel()).containsEntry("errorTitle", "Solicitud inválida");
        assertThat(mav.getModel()).containsEntry("errorMessage", "bad input");
    }

    @Test
    void handlesSecurityException() {
        ModelAndView mav = handler.handleSecurity(new SecurityException("forbidden /uploads/../secret"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(mav.getModel()).containsEntry("errorTitle", "Acceso denegado");
        assertThat(mav.getModel()).containsEntry("errorMessage", "La operación no está permitida.");
    }

    @Test
    void handlesRateLimitExceededException() {
        ModelAndView mav = handler.handleRateLimit(new RateLimitExceededException("too many"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(mav.getModel()).containsEntry("errorTitle", "Demasiadas solicitudes");
        assertThat(mav.getModel()).containsEntry("errorMessage", "too many");
    }

    @Test
    void handlesGenericException() {
        ModelAndView mav = handler.handleGeneric(new RuntimeException("unexpected"));

        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(mav.getModel()).containsEntry("errorTitle", "Error interno");
        assertThat(mav.getModel()).containsEntry("errorMessage", "Ocurrió un error inesperado. Inténtalo de nuevo.");
    }
}
