package com.gestorrh.api.dto.fichaje;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la petición de un fichaje de entrada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionFichajeEntradaDTO {

    @NotNull(message = "La latitud es obligatoria para fichar")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria para fichar")
    private Double longitud;
}