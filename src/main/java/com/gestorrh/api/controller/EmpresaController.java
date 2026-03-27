package com.gestorrh.api.controller;

import com.gestorrh.api.annotation.ApiErroresAccion;
import com.gestorrh.api.annotation.ApiErroresLectura;
import com.gestorrh.api.annotation.ApiErroresRegistro;
import com.gestorrh.api.dto.empresa.*;
import com.gestorrh.api.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión del perfil de Empresa y procesos de registro.
 * <p>
 * Proporciona endpoints para el alta de nuevas empresas, así como para la gestión
 * autónoma de su información de perfil, seguridad y baja del servicio.
 * </p>
 *
 */
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@Tag(name = "2. Empresas", description = "Gestión del perfil de empresa. Requiere Token con rol EMPRESA (excepto registro).")
public class EmpresaController {

    private final EmpresaService empresaService;

    /**
     * Registra una nueva empresa en el sistema.
     * <p>
     * Este endpoint es de acceso público y permite a nuevas organizaciones darse de alta en la plataforma.
     * </p>
     * <p>
     * URL de acceso: {@code POST http://localhost:8080/api/empresas/registro}
     * </p>
     * @param peticion DTO que contiene los datos de registro de la empresa (email, contraseña, nombre comercial, etc.).
     * @return ResponseEntity con el {@link RespuestaEmpresaDTO} de la empresa creada y estado HTTP 201 (Created).
     */
    @PostMapping("/registro")
    @Operation(
            summary = "Registrar nueva Empresa",
            description = "Endpoint público para dar de alta una nueva empresa en el sistema. No requiere autenticación.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Empresa registrada con éxito")
            }
    )
    @SecurityRequirements()
    @ApiErroresRegistro
    public ResponseEntity<RespuestaEmpresaDTO> registrarEmpresa(@Valid @RequestBody PeticionRegistroEmpresaDTO peticion) {
        RespuestaEmpresaDTO respuesta = empresaService.registrarEmpresa(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene la información detallada del perfil de la empresa que realiza la petición.
     * <p>
     * Requiere que el usuario esté autenticado con el rol 'EMPRESA'.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/empresas/me}
     * </p>
     * @return ResponseEntity con el {@link RespuestaEmpresaDTO} de la empresa autenticada.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(summary = "Obtener mi perfil", description = "Requiere Token de EMPRESA. Devuelve los datos de la empresa autenticada.")
    @ApiErroresLectura
    public ResponseEntity<RespuestaEmpresaDTO> obtenerMiPerfil() {
        return ResponseEntity.ok(empresaService.obtenerMiPerfil());
    }

    /**
     * Actualiza la información básica del perfil de la empresa autenticada.
     * <p>
     * Permite modificar campos como el nombre, teléfono o dirección de la organización.
     * </p>
     * <p>
     * URL de acceso: {@code PUT http://localhost:8080/api/empresas/me}
     * </p>
     * @param peticion DTO con los nuevos datos del perfil de la empresa.
     * @return ResponseEntity con el {@link RespuestaEmpresaDTO} actualizado y estado HTTP 200 (OK).
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(summary = "Actualizar mi perfil", description = "Requiere Token de EMPRESA. Modifica los datos básicos de la empresa.")
    @ApiErroresAccion
    public ResponseEntity<RespuestaEmpresaDTO> actualizarMiPerfil(@Valid @RequestBody PeticionActualizarEmpresaDTO peticion) {
        return ResponseEntity.ok(empresaService.actualizarMiPerfil(peticion));
    }

    /**
     * Cambia la contraseña de acceso de la empresa autenticada.
     * <p>
     * Requiere la validación de la contraseña actual por motivos de seguridad.
     * </p>
     * <p>
     * URL de acceso: {@code PUT http://localhost:8080/api/empresas/me/contrasena}
     * </p>
     * @param peticion DTO con la contraseña actual y la nueva contraseña deseada.
     * @return ResponseEntity con cuerpo vacío y estado HTTP 204 (No Content) tras el cambio exitoso.
     */
    @PutMapping("/me/contrasena")
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(summary = "Cambiar mi contraseña",
            description = "Requiere Token de EMPRESA. Cambia la contraseña de acceso actual.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Contraseña cambiada con éxito")
            }
    )
    @ApiErroresAccion
    public ResponseEntity<Void> cambiarMiContrasena(@Valid @RequestBody PeticionCambiarPasswordEmpresaDTO peticion) {
        empresaService.cambiarMiContrasena(peticion);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina de forma permanente la cuenta de la empresa autenticada del sistema.
     * <p>
     * Esta acción es irreversible y conlleva la eliminación de todos los datos asociados.
     * </p>
     * <p>
     * URL de acceso: {@code DELETE http://localhost:8080/api/empresas/me}
     * </p>
     * @return ResponseEntity con cuerpo vacío y estado HTTP 204 (No Content) tras la eliminación.
     */
    @DeleteMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(summary = "Eliminar mi empresa",
            description = "Requiere Token de EMPRESA. Borrado lógico/físico irreversible de la cuenta.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Empresa eliminada con éxito")
            }
    )
    @ApiErroresAccion
    public ResponseEntity<Void> eliminarMiEmpresa() {
        empresaService.eliminarMiEmpresa();
        return ResponseEntity.noContent().build();
    }
}
