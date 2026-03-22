package com.gestorrh.api.controller;

import com.gestorrh.api.dto.empresa.*;
import com.gestorrh.api.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión del perfil de Empresa y procesos de registro.
 * Proporciona endpoints para el alta de nuevas empresas, así como para la gestión
 * autónoma de su información de perfil, seguridad y baja del servicio.
 */
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    /**
     * Registra una nueva empresa en el sistema.
     * Este endpoint es de acceso público y permite a nuevas organizaciones darse de alta en la plataforma.
     *
     * URL de acceso: {@code POST http://localhost:8080/api/empresas/registro}
     *
     * @param peticion DTO que contiene los datos de registro de la empresa (email, contraseña, nombre comercial, etc.).
     * @return ResponseEntity con el {@link RespuestaEmpresaDTO} de la empresa creada y estado HTTP 201 (Created).
     */
    @PostMapping("/registro")
    public ResponseEntity<RespuestaEmpresaDTO> registrarEmpresa(@Valid @RequestBody PeticionRegistroEmpresaDTO peticion) {
        RespuestaEmpresaDTO respuesta = empresaService.registrarEmpresa(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene la información detallada del perfil de la empresa que realiza la petición.
     * Requiere que el usuario esté autenticado con el rol 'EMPRESA'.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/empresas/me}
     *
     * @return ResponseEntity con el {@link RespuestaEmpresaDTO} de la empresa autenticada.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<RespuestaEmpresaDTO> obtenerMiPerfil() {
        return ResponseEntity.ok(empresaService.obtenerMiPerfil());
    }

    /**
     * Actualiza la información básica del perfil de la empresa autenticada.
     * Permite modificar campos como el nombre, teléfono o dirección de la organización.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/empresas/me}
     *
     * @param peticion DTO con los nuevos datos del perfil de la empresa.
     * @return ResponseEntity con el {@link RespuestaEmpresaDTO} actualizado y estado HTTP 200 (OK).
     */
    @PutMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<RespuestaEmpresaDTO> actualizarMiPerfil(@Valid @RequestBody PeticionActualizarEmpresaDTO peticion) {
        return ResponseEntity.ok(empresaService.actualizarMiPerfil(peticion));
    }

    /**
     * Cambia la contraseña de acceso de la empresa autenticada.
     * Requiere la validación de la contraseña actual por motivos de seguridad.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/empresas/me/contrasena}
     *
     * @param peticion DTO con la contraseña actual y la nueva contraseña deseada.
     * @return ResponseEntity con cuerpo vacío y estado HTTP 204 (No Content) tras el cambio exitoso.
     */
    @PutMapping("/me/contrasena")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<Void> cambiarMiContrasena(@Valid @RequestBody PeticionCambiarPasswordEmpresaDTO peticion) {
        empresaService.cambiarMiContrasena(peticion);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina de forma permanente la cuenta de la empresa autenticada del sistema.
     * Esta acción es irreversible y conlleva la eliminación de todos los datos asociados.
     *
     * URL de acceso: {@code DELETE http://localhost:8080/api/empresas/me}
     *
     * @return ResponseEntity con cuerpo vacío y estado HTTP 204 (No Content) tras la eliminación.
     */
    @DeleteMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<Void> eliminarMiEmpresa() {
        empresaService.eliminarMiEmpresa();
        return ResponseEntity.noContent().build();
    }
}
