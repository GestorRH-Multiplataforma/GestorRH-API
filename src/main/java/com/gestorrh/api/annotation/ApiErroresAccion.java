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
@ApiResponse(responseCode = "400", description = "Regla de negocio no cumplida",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 400,\n  \"errorCode\": \"BAD_REQUEST\",\n  \"message\": \"La acción no se puede realizar.\"\n}")))
@ApiResponse(responseCode = "401", description = "No autorizado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 401,\n  \"errorCode\": \"UNAUTHORIZED\",\n  \"message\": \"Token JWT ausente.\"\n}")))
@ApiResponse(responseCode = "403", description = "Prohibido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 403,\n  \"errorCode\": \"FORBIDDEN\",\n  \"message\": \"Acceso denegado.\"\n}")))
@ApiResponse(responseCode = "404", description = "Recurso no encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"errorCode\": \"NOT_FOUND\",\n  \"message\": \"El recurso sobre el que quieres actuar no existe.\"\n}")))
@ApiResponse(responseCode = "409", description = "Conflicto de concurrencia",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 409,\n  \"errorCode\": \"CONFLICT\",\n  \"message\": \"Los datos han sido modificados por otro usuario mientras los visualizabas. Por favor, recarga la página e inténtalo de nuevo.\"\n}")))
@ApiResponse(responseCode = "500", description = "Error interno",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = RespuestaErrorDTO.class),
                examples = @ExampleObject(value = "{\n  \"status\": 500,\n  \"errorCode\": \"INTERNAL_SERVER_ERROR\",\n  \"message\": \"Error inesperado.\"\n}")))
public @interface ApiErroresAccion {}
