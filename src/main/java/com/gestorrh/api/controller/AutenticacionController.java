package com.gestorrh.api.controller;

import com.gestorrh.api.dto.autenticacion.PeticionLoginDTO;
import com.gestorrh.api.dto.autenticacion.RespuestaLoginDTO;
import com.gestorrh.api.service.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que gestiona los procesos de autenticación y acceso al sistema.
 * <p>
 * Proporciona endpoints específicos para el inicio de sesión diferenciado entre
 * perfiles de Empresa y perfiles de Empleado, retornando los tokens de acceso necesarios.
 * </p>
 *
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "1. Autenticación", description = "Endpoints públicos para iniciar sesión y obtener los tokens JWT de acceso.")
public class AutenticacionController {

    private final AutenticacionService servicioAutenticacion;

    /**
     * Gestiona el inicio de sesión para usuarios con perfil de Empresa.
     * <p>
     * Valida las credenciales proporcionadas y, en caso de éxito, devuelve un token JWT.
     * </p>
     * <p>
     * URL de acceso: {@code POST http://localhost:8080/auth/login-empresa}
     * </p>
     *
     * @param peticion DTO que contiene las credenciales de acceso (email y contraseña).
     * @return ResponseEntity con el objeto {@link RespuestaLoginDTO} que incluye el token y datos básicos, y estado 200 (OK).
     */
    @PostMapping("/login-empresa")
    @Operation(
            summary = "Login de Empresa",
            description = "Punto de entrada público. Introduce credenciales de empresa para obtener un Token con rol EMPRESA."
    )
    @SecurityRequirements()
    public ResponseEntity<RespuestaLoginDTO> loginEmpresa(@Valid @RequestBody PeticionLoginDTO peticion) {

        RespuestaLoginDTO respuesta = servicioAutenticacion.loginEmpresa(peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Gestiona el inicio de sesión para usuarios con perfil de Empleado o Supervisor.
     * <p>
     * Valida las credenciales proporcionadas y, en caso de éxito, devuelve un token JWT.
     * </p>
     * <p>
     * URL de acceso: {@code POST http://localhost:8080/auth/login-empleado}
     * </p>
     *
     * @param peticion DTO que contiene las credenciales de acceso (email y contraseña).
     * @return ResponseEntity con el objeto {@link RespuestaLoginDTO} que incluye el token y datos básicos, y estado 200 (OK).
     */
    @PostMapping("/login-empleado")
    @Operation(
            summary = "Login de Empleado / Supervisor",
            description = "Punto de entrada público. Introduce credenciales de empleado para obtener un Token con rol EMPLEADO o SUPERVISOR."
    )
    @SecurityRequirements()
    public ResponseEntity<RespuestaLoginDTO> loginEmpleado(@Valid @RequestBody PeticionLoginDTO peticion) {

        RespuestaLoginDTO respuesta = servicioAutenticacion.loginEmpleado(peticion);
        return ResponseEntity.ok(respuesta);
    }
}
