package com.gestorrh.api.dto.empleado;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la petición de baja voluntaria o despido de un empleado.
 */
@Data
public class PeticionBajaEmpleadoDTO {
    @NotNull(message = "La fecha de baja es obligatoria")
    private LocalDate fechaBajaContrato;
}
