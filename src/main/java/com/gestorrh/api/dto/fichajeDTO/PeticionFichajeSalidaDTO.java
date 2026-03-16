package com.gestorrh.api.dto.fichajeDTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionFichajeSalidaDTO {

    @NotNull(message = "La latitud es obligatoria para fichar la salida")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria para fichar la salida")
    private Double longitud;
}
