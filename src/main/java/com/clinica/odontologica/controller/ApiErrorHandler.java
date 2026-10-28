package com.clinica.odontologica.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.clinica.odontologica.controller")
public class ApiErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> erro(ResponseStatusException e, HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/api/")) throw e;
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("erro", e.getReason() == null ? "Erro" : e.getReason()));
    }
}
