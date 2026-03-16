package com.gestorrh.api.controller;

import com.gestorrh.api.dto.empresaDTO.*;
import com.gestorrh.api.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    // ENDPOINT PÚBLICO (No requiere Token)

    @PostMapping("/registro")
    public ResponseEntity<RespuestaEmpresaDTO> registrarEmpresa(@Valid @RequestBody PeticionRegistroEmpresaDTO peticion) {
        RespuestaEmpresaDTO respuesta = empresaService.registrarEmpresa(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // ENDPOINTS PRIVADOS (Requieren Token de Empresa)

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<RespuestaEmpresaDTO> obtenerMiPerfil() {
        return ResponseEntity.ok(empresaService.obtenerMiPerfil());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<RespuestaEmpresaDTO> actualizarMiPerfil(@Valid @RequestBody PeticionActualizarEmpresaDTO peticion) {
        return ResponseEntity.ok(empresaService.actualizarMiPerfil(peticion));
    }

    @PutMapping("/me/contrasena")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<Void> cambiarMiContrasena(@Valid @RequestBody PeticionCambiarPasswordEmpresaDTO peticion) {
        empresaService.cambiarMiContrasena(peticion);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<Void> eliminarMiEmpresa() {
        empresaService.eliminarMiEmpresa();
        return ResponseEntity.noContent().build();
    }
}
