package com.gestorrh.api.dto.ausencia;

import com.gestorrh.api.entity.enums.EstadoAusencia;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para la revisión (aprobación/rechazo) de una ausencia.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeticionRevisionAusenciaDTO {

    @NotNull(message = "El nuevo estado es obligatorio (APROBADA o RECHAZADA)")
    private EstadoAusencia estado;

    private String observacionesRevision;
}
