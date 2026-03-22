package com.gestorrh.api.controller;

import com.gestorrh.api.dto.fichaje.PeticionFichajeEntradaDTO;
import com.gestorrh.api.dto.fichaje.PeticionFichajeSalidaDTO;
import com.gestorrh.api.dto.fichaje.RespuestaFichajeDTO;
import com.gestorrh.api.service.FichajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fichajes")
@RequiredArgsConstructor
public class FichajeController {

    private final FichajeService fichajeService;

    // ENDPOINTS DE FICHAJE (MÓVIL)

    @PostMapping("/entrada")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR')")
    public ResponseEntity<RespuestaFichajeDTO> ficharEntrada(@Valid @RequestBody PeticionFichajeEntradaDTO peticion) {
        RespuestaFichajeDTO respuesta = fichajeService.ficharEntrada(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping("/salida")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR')")
    public ResponseEntity<RespuestaFichajeDTO> ficharSalida(@Valid @RequestBody PeticionFichajeSalidaDTO peticion) {
        RespuestaFichajeDTO respuesta = fichajeService.ficharSalida(peticion);
        return ResponseEntity.ok(respuesta);
    }

    // ENDPOINT DE CONSULTAS (ESCRITORIO Y MÓVIL)

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR', 'EMPRESA')")
    public ResponseEntity<List<RespuestaFichajeDTO>> consultarFichajes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long empleadoId) {

        List<RespuestaFichajeDTO> historial = fichajeService.consultarFichajes(fechaInicio, fechaFin, empleadoId);
        return ResponseEntity.ok(historial);
    }

    /**
     * Permite a un Supervisor o Empresa modificar la hora de entrada/salida de un fichaje.
     * Deja un rastro de auditoría en el campo incidencias.
     * URL: PUT http://localhost:8080/api/fichajes/1/modificar
     */
    @PutMapping("/{idFichaje}/modificar")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<RespuestaFichajeDTO> modificarFichajeManual(
            @PathVariable Long idFichaje,
            @Valid @RequestBody com.gestorrh.api.dto.fichaje.PeticionModificacionFichajeDTO peticion) {

        RespuestaFichajeDTO respuesta = fichajeService.modificarFichajeManual(idFichaje, peticion);
        return ResponseEntity.ok(respuesta);
    }
}
