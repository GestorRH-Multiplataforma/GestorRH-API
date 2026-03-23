package com.gestorrh.api.controller;

import com.gestorrh.api.dto.fichaje.PeticionFichajeEntradaDTO;
import com.gestorrh.api.dto.fichaje.PeticionFichajeSalidaDTO;
import com.gestorrh.api.dto.fichaje.RespuestaFichajeDTO;
import com.gestorrh.api.service.FichajeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * <p>
 * Proporciona funcionalidades para registrar entradas y salidas, consultar el historial
 * de fichajes y realizar modificaciones manuales por parte de perfiles autorizados.
 * </p>
 */
@RestController
@RequestMapping("/api/fichajes")
@RequiredArgsConstructor
@Tag(
        name = "6. Fichajes",
        description = "Control horario y registro de jornada diaria. Gestión de entradas, salidas y correcciones manuales para auditoría."
)
public class FichajeController {

    private final FichajeService fichajeService;

    /**
     * Registra el inicio de la jornada laboral de un empleado (fichaje de entrada).
     * <p>
     * Este endpoint es utilizado principalmente por la aplicación móvil.
     * </p>
     * <p>
     * URL de acceso: {@code POST http://localhost:8080/api/fichajes/entrada}
     * </p>
     *
     * @param peticion DTO con la información necesaria para el registro de entrada.
     * @return ResponseEntity con el {@link RespuestaFichajeDTO} generado y estado HTTP 201 (Created).
     */
    @PostMapping("/entrada")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR')")
    @Operation(
            summary = "Fichar entrada",
            description = "Requiere Token de EMPLEADO o SUPERVISOR. Registra el inicio de la jornada laboral del usuario autenticado."
    )
    public ResponseEntity<RespuestaFichajeDTO> ficharEntrada(@Valid @RequestBody PeticionFichajeEntradaDTO peticion) {
        RespuestaFichajeDTO respuesta = fichajeService.ficharEntrada(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Registra el fin de la jornada laboral de un empleado (fichaje de salida).
     * <p>
     * Actualiza el registro de entrada correspondiente con la hora de salida y calcula la jornada.
     * </p>
     * <p>
     * URL de acceso: {@code PUT http://localhost:8080/api/fichajes/salida}
     * </p>
     *
     * @param peticion DTO con la información necesaria para el registro de salida.
     * @return ResponseEntity con el {@link RespuestaFichajeDTO} actualizado y estado HTTP 200 (OK).
     */
    @PutMapping("/salida")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR')")
    @Operation(
            summary = "Fichar salida",
            description = "Requiere Token de EMPLEADO o SUPERVISOR. Cierra el fichaje abierto del usuario autenticado y calcula el tiempo trabajado."
    )
    public ResponseEntity<RespuestaFichajeDTO> ficharSalida(@Valid @RequestBody PeticionFichajeSalidaDTO peticion) {
        RespuestaFichajeDTO respuesta = fichajeService.ficharSalida(peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Permite consultar el historial de fichajes realizados en un rango de fechas determinado.
     * <p>
     * Puede ser filtrado opcionalmente por un empleado específico si el usuario tiene permisos.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/fichajes?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     * </p>
     *
     * @param fechaInicio Fecha de inicio para el rango de búsqueda.
     * @param fechaFin Fecha de fin para el rango de búsqueda.
     * @param empleadoId Identificador opcional del empleado a consultar.
     * @return ResponseEntity con la lista de {@link RespuestaFichajeDTO} encontrados y estado HTTP 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'SUPERVISOR', 'EMPRESA')")
    @Operation(
            summary = "Consultar historial de fichajes",
            description = "Requiere Token de EMPRESA, SUPERVISOR o EMPLEADO. Filtra fichajes por fechas. " +
                    "Los EMPLEADOS solo ven los suyos; SUPERVISOR/EMPRESA pueden usar el parámetro opcional 'empleadoId' " +
                    "para ver el historial de un trabajador específico."
    )
    public ResponseEntity<List<RespuestaFichajeDTO>> consultarFichajes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long empleadoId) {

        List<RespuestaFichajeDTO> historial = fichajeService.consultarFichajes(fechaInicio, fechaFin, empleadoId);
        return ResponseEntity.ok(historial);
    }

    /**
     * Permite a un Supervisor o Administrador de Empresa modificar manualmente la hora de entrada o salida de un fichaje.
     * <p>
     * Esta acción queda registrada para auditoría a través de un campo de incidencias.
     * </p>
     * <p>
     * URL de acceso: {@code PUT http://localhost:8080/api/fichajes/{idFichaje}/modificar}
     * </p>
     *
     * @param idFichaje Identificador único del fichaje que se desea corregir.
     * @param peticion DTO con los nuevos datos de tiempo y el motivo de la modificación.
     * @return ResponseEntity con el {@link RespuestaFichajeDTO} modificado y estado HTTP 200 (OK).
     */
    @PutMapping("/{idFichaje}/modificar")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    @Operation(
            summary = "Modificar fichaje manualmente",
            description = "Requiere Token de EMPRESA o SUPERVISOR. Permite corregir horas de entrada/salida de un fichaje existente. " +
                    "Quedará registrado el motivo de la modificación por motivos legales y de auditoría."
    )
    public ResponseEntity<RespuestaFichajeDTO> modificarFichajeManual(
            @PathVariable Long idFichaje,
            @Valid @RequestBody com.gestorrh.api.dto.fichaje.PeticionModificacionFichajeDTO peticion) {

        RespuestaFichajeDTO respuesta = fichajeService.modificarFichajeManual(idFichaje, peticion);
        return ResponseEntity.ok(respuesta);
    }
}
