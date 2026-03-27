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
@ApiResponse(responseCode = "401", description = "No autorizado - Token ausente o inválido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"errorCode\": \"UNAUTHORIZED\",\n  \"message\": \"Token JWT ausente o inválido.\"\n}")))
@ApiResponse(responseCode = "403", description = "Prohibido - No tienes permisos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 403,\n  \"errorCode\": \"FORBIDDEN\",\n  \"message\": \"Acceso denegado. No tienes el rol necesario.\"\n}")))
@ApiResponse(responseCode = "404", description = "Recurso no encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"errorCode\": \"NOT_FOUND\",\n  \"message\": \"El recurso solicitado no existe.\"\n}")))
@ApiResponse(responseCode = "500", description = "Error interno del servidor",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 500,\n  \"errorCode\": \"INTERNAL_SERVER_ERROR\",\n  \"message\": \"Error inesperado.\"\n}")))
public @interface ApiErroresLectura {}
