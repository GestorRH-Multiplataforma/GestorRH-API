package com.gestorrh.api.exception;

import com.gestorrh.api.dto.RespuestaErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestor global de excepciones para interceptar errores y devolver un JSON limpio.
 */
@RestControllerAdvice
public class GestorExcepciones {

    /**
     * Captura los errores de validación de los DTOs (ej. @Email, @NotBlank).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaErrorDTO> manejarValidaciones(MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> errores = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        RespuestaErrorDTO errorResponse = RespuestaErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_ERROR")
                .message("Error en la validación de los datos enviados")
                .path(request.getRequestURI())
                .details(errores)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Captura los errores de lógica de negocio (los RuntimeException que lanzas en los Services).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RespuestaErrorDTO> manejarExcepcionesDeNegocio(RuntimeException ex, HttpServletRequest request) {

        RespuestaErrorDTO errorResponse = RespuestaErrorDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("BUSINESS_RULE_VIOLATION")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(List.of("Revisa las reglas de negocio o los parámetros enviados"))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
