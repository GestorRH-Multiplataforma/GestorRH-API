package com.gestorrh.api.dto.ausencia;

import com.gestorrh.api.entity.enums.EstadoAusencia;
import com.gestorrh.api.entity.enums.TipoAusencia;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO para la respuesta con los datos detallados de una ausencia.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaAusenciaDTO {

    private Long idAusencia;
    private Long idEmpleado;
    private String nombreCompletoEmpleado;
    private TipoAusencia tipo;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    @Schema(
            description = "Nombre del archivo adjunto (UUID + extensión) almacenado en el servidor. " +
                    "Es `null` si la ausencia no tiene justificante. El valor debe usarse directamente como " +
                    "parámetro `{nombreArchivo}` en `GET /api/ausencias/justificantes/{nombreArchivo}` para descargarlo.",
            example = "a3f5c0e2-9b1d-4f7c-8e6a-2d1b0c4f5a6b.pdf",
            nullable = true
    )
    private String justificante;
    private EstadoAusencia estado;

    private String responsableRevision;
    private String observacionesRevision;
}
