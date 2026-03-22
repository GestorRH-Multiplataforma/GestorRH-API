package com.gestorrh.api.exception;

import com.gestorrh.api.dto.error.RespuestaErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
 * Clase controladora de asesoramiento global para la gestión de excepciones en la API.
 * <p>
 * Centraliza la captura de errores lanzados desde cualquier capa del sistema (controladores, servicios, etc.)
 * y los transforma en una respuesta JSON estandarizada mediante {@link RespuestaErrorDTO}.
 * Utiliza la anotación {@code @RestControllerAdvice} para interceptar las excepciones de forma transversal.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GestorExcepciones {

    /**
     * Maneja las excepciones producidas cuando falla la validación de los argumentos de entrada (anotaciones {@code @Valid}).
     * <p>
     * Extrae todos los mensajes de error asociados a los campos del DTO que no han superado las restricciones
     * y genera una respuesta {@link HttpStatus#BAD_REQUEST} detallada.
     * </p>
     *
     * @param ex La excepción de validación capturada.
     * @param request La solicitud HTTP en la que se produjo el error, para extraer la URI.
     * @return Una respuesta estructurada con los detalles de los campos que fallaron la validación.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaErrorDTO> manejarValidaciones(MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> errores = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.error("Error de VALIDACIÓN en la ruta [{}]: {}", request.getRequestURI(), errores);

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
     * Maneja las excepciones genéricas de ejecución que representan violaciones de reglas de negocio.
     * <p>
     * Captura cualquier {@link RuntimeException} no manejada específicamente, registrando el error en el log
     * y retornando una respuesta {@link HttpStatus#BAD_REQUEST} con el mensaje descriptivo de la excepción.
     * </p>
     *
     * @param ex La excepción de negocio o error de ejecución capturado.
     * @param request La solicitud HTTP donde ocurrió la incidencia.
     * @return Una respuesta de error estandarizada con el mensaje de la excepción.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RespuestaErrorDTO> manejarExcepcionesDeNegocio(RuntimeException ex, HttpServletRequest request) {

        log.warn("Violación de regla de NEGOCIO en la ruta [{}]: {}", request.getRequestURI(), ex.getMessage());

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
