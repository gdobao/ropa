package com.colorinchi.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleBadRequest(IllegalArgumentException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        mav.addObject("errorTitle", "Solicitud inválida");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(SecurityException.class)
    public ModelAndView handleSecurity(SecurityException ex) {
        log.warn("Rejected MVC request: {}", ex.getClass().getSimpleName());
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.FORBIDDEN);
        mav.addObject("errorTitle", "Acceso denegado");
        mav.addObject("errorMessage", "La operación no está permitida.");
        return mav;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ModelAndView handleRateLimit(RateLimitExceededException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.TOO_MANY_REQUESTS);
        mav.addObject("errorTitle", "Demasiadas solicitudes");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        log.error("Unhandled MVC exception", ex);
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "Error interno");
        mav.addObject("errorMessage", "Ocurrió un error inesperado. Intenta de nuevo.");
        return mav;
    }
}
