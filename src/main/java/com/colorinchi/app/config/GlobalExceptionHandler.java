package com.colorinchi.app.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleBadRequest(IllegalArgumentException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorTitle", "Solicitud inválida");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(SecurityException.class)
    public ModelAndView handleSecurity(SecurityException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorTitle", "Acceso denegado");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ModelAndView handleRateLimit(RateLimitExceededException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorTitle", "Demasiadas solicitudes");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        // log the stack trace
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorTitle", "Error interno");
        mav.addObject("errorMessage", "Ocurrió un error inesperado. Intenta de nuevo.");
        return mav;
    }
}
