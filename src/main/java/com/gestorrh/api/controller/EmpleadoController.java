package com.gestorrh.api.controller;

import com.gestorrh.api.dto.PeticionCrearEmpleadoDTO;
import com.gestorrh.api.dto.RespuestaCrearEmpleadoDTO;
import com.gestorrh.api.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestión de Empleados.
 * Todos los endpoints de esta clase requerirán un token válido.
 */
@RestController
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    /**
     * Endpoint para dar de alta a un nuevo empleado.
     * SOLO accesible para usuarios con el rol EMPRESA.
     * URL: POST http://localhost:8080/api/empleados
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<RespuestaCrearEmpleadoDTO> crearEmpleado(
            @Valid @RequestBody PeticionCrearEmpleadoDTO peticion) {

        RespuestaCrearEmpleadoDTO respuesta = empleadoService.crearEmpleado(peticion);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
