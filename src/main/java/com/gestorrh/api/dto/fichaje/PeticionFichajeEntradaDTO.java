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

    private Double latitud;

    private Double longitud;
}