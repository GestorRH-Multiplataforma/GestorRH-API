package com.gestorrh.api.controller;

import com.gestorrh.api.annotation.ApiErroresEscritura;
import com.gestorrh.api.dto.empleado.PeticionResetPasswordDTO;
import com.gestorrh.api.dto.empleado.RespuestaEmpleadoDTO;
import com.gestorrh.api.service.EmpleadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para operaciones administrativas de Recursos Humanos sobre el personal.
 * <p>
 * Agrupa acciones reservadas al rol {@code EMPRESA} que no encajan en el flujo habitual de gestión
 * del empleado, como el restablecimiento de credenciales cuando un trabajador ha olvidado su contraseña.
 * La política de seguridad interna prohíbe la auto-recuperación por correo, por lo que estas operaciones
 * deben ser siempre ejecutadas de forma centralizada por personal autorizado.
 * </p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "9. Administración RRHH",
        description = "Operaciones administrativas centralizadas para el personal de RRHH (rol EMPRESA), " +
                "como el restablecimiento de contraseñas de empleados.")
public class AdminController {

    private final EmpleadoService empleadoService;

    /**
     * Restablece la contraseña de un empleado cuando este la ha olvidado.
     * <p>
     * Únicamente accesible para usuarios con el rol de 'EMPRESA'. No se exige la contraseña anterior:
     * se sustituye directamente por la nueva, que se almacena cifrada con BCrypt.
     * </p>
     * <p>
     * URL de acceso: {@code PUT http://localhost:8080/api/admin/empleados/{idEmpleado}/reset-password}
     * </p>
     *
     * @param idEmpleado Identificador del empleado cuya contraseña se va a restablecer.
     * @param peticion DTO con la nueva contraseña (mínimo 8 caracteres).
     * @return ResponseEntity con el {@link RespuestaEmpleadoDTO} actualizado y estado 200 (OK).
     */
    @PutMapping("/empleados/{idEmpleado}/reset-password")
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(
            summary = "Restablecer contraseña de empleado (RRHH)",
            description = "Requiere Token de EMPRESA. Permite al personal de RRHH establecer una nueva " +
                    "contraseña para un empleado que la ha olvidado. La contraseña nunca se devuelve en la respuesta."
    )
    @ApiErroresEscritura
    public ResponseEntity<RespuestaEmpleadoDTO> resetPasswordEmpleado(
            @PathVariable Long idEmpleado,
            @Valid @RequestBody PeticionResetPasswordDTO peticion) {

        RespuestaEmpleadoDTO respuesta = empleadoService.resetPassword(idEmpleado, peticion.getNuevaPassword());
        return ResponseEntity.ok(respuesta);
    }
}
