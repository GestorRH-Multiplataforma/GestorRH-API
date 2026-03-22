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

/**
 * Controlador REST para la gestión de los registros de jornada laboral (fichajes).
 * Proporciona funcionalidades para registrar entradas y salidas, consultar el historial
 * de fichajes y realizar modificaciones manuales por parte de perfiles autorizados.
 */
@RestController
@RequestMapping("/api/fichajes")
@RequiredArgsConstructor
public class FichajeController {

    private final FichajeService fichajeService;

    /**
     * Registra el inicio de la jornada laboral de un empleado (fichaje de entrada).
     * Este endpoint es utilizado principalmente por la aplicación móvil.
     *
     * URL de acceso: {@code POST http://localhost:8080/api/fichajes/entrada}
     *
     * @param peticion DTO con la información necesaria para el registro de entrada.
     * @return ResponseEntity con el {@link RespuestaFichajeDTO} generado y estado HTTP 201 (Created).
     */
    @PostMapping("/entrada")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR')")
    public ResponseEntity<RespuestaFichajeDTO> ficharEntrada(@Valid @RequestBody PeticionFichajeEntradaDTO peticion) {
        RespuestaFichajeDTO respuesta = fichajeService.ficharEntrada(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Registra el fin de la jornada laboral de un empleado (fichaje de salida).
     * Actualiza el registro de entrada correspondiente con la hora de salida y calcula la jornada.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/fichajes/salida}
     *
     * @param peticion DTO con la información necesaria para el registro de salida.
     * @return ResponseEntity con el {@link RespuestaFichajeDTO} actualizado y estado HTTP 200 (OK).
     */
    @PutMapping("/salida")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR')")
    public ResponseEntity<RespuestaFichajeDTO> ficharSalida(@Valid @RequestBody PeticionFichajeSalidaDTO peticion) {
        RespuestaFichajeDTO respuesta = fichajeService.ficharSalida(peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Permite consultar el historial de fichajes realizados en un rango de fechas determinado.
     * Puede ser filtrado opcionalmente por un empleado específico si el usuario tiene permisos.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/fichajes?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     *
     * @param fechaInicio Fecha de inicio para el rango de búsqueda.
     * @param fechaFin Fecha de fin para el rango de búsqueda.
     * @param empleadoId Identificador opcional del empleado a consultar.
     * @return ResponseEntity con la lista de {@link RespuestaFichajeDTO} encontrados y estado HTTP 200 (OK).
     */
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
     * Permite a un Supervisor o Administrador de Empresa modificar manualmente la hora de entrada o salida de un fichaje.
     * Esta acción queda registrada para auditoría a través de un campo de incidencias.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/fichajes/{idFichaje}/modificar}
     *
     * @param idFichaje Identificador único del fichaje que se desea corregir.
     * @param peticion DTO con los nuevos datos de tiempo y el motivo de la modificación.
     * @return ResponseEntity con el {@link RespuestaFichajeDTO} modificado y estado HTTP 200 (OK).
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
