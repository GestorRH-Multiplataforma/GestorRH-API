package com.gestorrh.api.dto.empleado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la petición de restablecimiento de contraseña por parte de RRHH (rol EMPRESA).
 * <p>
 * A diferencia de {@link PeticionCambiarPasswordDTO}, esta operación administrativa no requiere
 * conocer la contraseña actual del empleado, ya que su caso de uso típico es el olvido de credenciales.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionResetPasswordDTO {

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String nuevaPassword;
}
