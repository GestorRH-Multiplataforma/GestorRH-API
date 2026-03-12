package com.gestorrh.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO que estandariza las respuestas de error de la API (Issue P0-31).
 */
@Getter
@Builder
public class RespuestaErrorDTO {
    private LocalDateTime timestamp;
    private int status;
    private String errorCode;
    private String message;
    private String path;
    private List<String> details;
}
