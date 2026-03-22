package com.gestorrh.api.dto.empresa;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la petición de actualización de datos de una empresa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionActualizarEmpresaDTO {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    private String nombre;

    @NotBlank(message = "La dirección de la sede es obligatoria")
    private String direccion;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    private Double latitudSede;
    private Double longitudSede;
    private Integer radioValidez;
}
