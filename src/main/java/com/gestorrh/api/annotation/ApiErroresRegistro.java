package com.gestorrh.api.annotation;

import com.gestorrh.api.dto.error.RespuestaErrorDTO;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = "400", description = "Error de validación (Ej. Email ya registrado, formato incorrecto)",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 400,\n  \"errorCode\": \"BAD_REQUEST\",\n  \"message\": \"El email ya está en uso.\"\n}")))
@ApiResponse(responseCode = "500", description = "Error interno del servidor",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 500,\n  \"errorCode\": \"INTERNAL_SERVER_ERROR\",\n  \"message\": \"Error inesperado.\"\n}")))
public @interface ApiErroresRegistro {}
