package com.gestorrh.api.dto.fichaje;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para solicitar la modificación de un fichaje existente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionModificacionFichajeDTO {

    private LocalDateTime nuevaHoraEntrada;
    private LocalDateTime nuevaHoraSalida;

    @NotBlank(message = "El motivo de la modificación es obligatorio por motivos de auditoría")
    private String motivoModificacion;
}
