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
@ApiResponse(responseCode = "400", description = "Error de validación o regla de negocio",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 400,\n  \"errorCode\": \"BAD_REQUEST\",\n  \"message\": \"Error en los datos enviados o regla de negocio.\"\n}")))
@ApiResponse(responseCode = "401", description = "No autorizado - Token ausente o inválido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"errorCode\": \"UNAUTHORIZED\",\n  \"message\": \"Token JWT ausente o inválido.\"\n}")))
@ApiResponse(responseCode = "403", description = "Prohibido - No tienes permisos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 403,\n  \"errorCode\": \"FORBIDDEN\",\n  \"message\": \"Acceso denegado. No tienes el rol necesario.\"\n}")))
@ApiResponse(responseCode = "409", description = "Conflicto de concurrencia",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 409,\n  \"errorCode\": \"CONFLICT\",\n  \"message\": \"Los datos han sido modificados por otro usuario mientras los visualizabas. Por favor, recarga la página e inténtalo de nuevo.\"\n}")))
@ApiResponse(responseCode = "500", description = "Error interno del servidor",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 500,\n  \"errorCode\": \"INTERNAL_SERVER_ERROR\",\n  \"message\": \"Error inesperado.\"\n}")))
public @interface ApiErroresEscritura {}
