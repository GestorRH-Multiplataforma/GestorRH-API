package com.gestorrh.api.controller;

import com.gestorrh.api.dto.autenticacion.PeticionLoginDTO;
import com.gestorrh.api.dto.autenticacion.RespuestaLoginDTO;
import com.gestorrh.api.service.AutenticacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que gestiona los procesos de autenticación y acceso al sistema.
 * Proporciona endpoints específicos para el inicio de sesión diferenciado entre
 * perfiles de Empresa y perfiles de Empleado, retornando los tokens de acceso necesarios.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionService servicioAutenticacion;

    /**
     * Gestiona el inicio de sesión para usuarios con perfil de Empresa.
     * Valida las credenciales proporcionadas y, en caso de éxito, devuelve un token JWT.
     *
     * URL de acceso: {@code POST http://localhost:8080/auth/login-empresa}
     *
     * @param peticion DTO que contiene las credenciales de acceso (email y contraseña).
     * @return ResponseEntity con el objeto {@link RespuestaLoginDTO} que incluye el token y datos básicos, y estado 200 (OK).
     */
    @PostMapping("/login-empresa")
    public ResponseEntity<RespuestaLoginDTO> loginEmpresa(@Valid @RequestBody PeticionLoginDTO peticion) {

        RespuestaLoginDTO respuesta = servicioAutenticacion.loginEmpresa(peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Gestiona el inicio de sesión para usuarios con perfil de Empleado o Supervisor.
     * Valida las credenciales proporcionadas y, en caso de éxito, devuelve un token JWT.
     *
     * URL de acceso: {@code POST http://localhost:8080/auth/login-empleado}
     *
     * @param peticion DTO que contiene las credenciales de acceso (email y contraseña).
     * @return ResponseEntity con el objeto {@link RespuestaLoginDTO} que incluye el token y datos básicos, y estado 200 (OK).
     */
    @PostMapping("/login-empleado")
    public ResponseEntity<RespuestaLoginDTO> loginEmpleado(@Valid @RequestBody PeticionLoginDTO peticion) {

        RespuestaLoginDTO respuesta = servicioAutenticacion.loginEmpleado(peticion);
        return ResponseEntity.ok(respuesta);
    }
}
