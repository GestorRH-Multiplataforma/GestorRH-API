package com.gestorrh.api.controller;

import com.gestorrh.api.dto.asignacion.PeticionAsignacionTurnoDTO;
import com.gestorrh.api.dto.asignacion.RespuestaAsignacionTurnoDTO;
import com.gestorrh.api.entity.enums.ModalidadTurno;
import com.gestorrh.api.service.AsignacionTurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Controlador REST para la gestión de asignaciones de turnos de trabajo a empleados.
 * Permite la planificación de horarios, la consulta de asignaciones propias y generales,
 * así como la modificación y eliminación de estas por parte de usuarios autorizados.
 */
@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionTurnoController {

    private final AsignacionTurnoService asignacionService;

    /**
     * Registra una nueva asignación de turno para un empleado específico.
     * Este endpoint es utilizado por perfiles con capacidad de planificación (Empresa o Supervisores).
     *
     * URL de acceso: {@code POST http://localhost:8080/api/asignaciones}
     *
     * @param peticion DTO con los detalles de la asignación (empleado, turno, fecha, modalidad).
     * @return ResponseEntity con el {@link RespuestaAsignacionTurnoDTO} creado y estado 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<RespuestaAsignacionTurnoDTO> crearAsignacion(@Valid @RequestBody PeticionAsignacionTurnoDTO peticion) {
        RespuestaAsignacionTurnoDTO respuesta = asignacionService.crearAsignacion(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Obtiene el listado de asignaciones de turno según los permisos del usuario.
     * Las organizaciones con rol 'EMPRESA' visualizan todas las asignaciones, mientras
     * que los supervisores acceden a las de su ámbito de responsabilidad.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/asignaciones}
     *
     * @return ResponseEntity con la lista de {@link RespuestaAsignacionTurnoDTO} permitidas.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<RespuestaAsignacionTurnoDTO>> listarAsignaciones() {
        List<RespuestaAsignacionTurnoDTO> lista = asignacionService.obtenerAsignacionesPermitidas();
        return ResponseEntity.ok(lista);
    }

    /**
     * Recupera el listado exclusivo de asignaciones de turno para el empleado autenticado.
     * Permite al trabajador conocer su planificación horaria personal.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/asignaciones/me}
     *
     * @return ResponseEntity con la lista de {@link RespuestaAsignacionTurnoDTO} del usuario logueado.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<List<RespuestaAsignacionTurnoDTO>> obtenerMisAsignaciones() {
        List<RespuestaAsignacionTurnoDTO> lista = asignacionService.obtenerMisAsignaciones();
        return ResponseEntity.ok(lista);
    }

    /**
     * Modifica una asignación de turno previamente registrada.
     * Requiere la especificación de un motivo para el cambio con fines de auditoría.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/asignaciones/{id}}
     *
     * @param id Identificador único de la asignación a modificar.
     * @param peticion DTO con los nuevos datos de la asignación y el motivo del cambio.
     * @return ResponseEntity con el {@link RespuestaAsignacionTurnoDTO} actualizado.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<RespuestaAsignacionTurnoDTO> actualizarAsignacion(
            @PathVariable("id") Long id,
            @Valid @RequestBody PeticionAsignacionTurnoDTO peticion) {
        RespuestaAsignacionTurnoDTO respuesta = asignacionService.actualizarAsignacion(id, peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Elimina de forma permanente una asignación de turno del sistema.
     * Esta operación es destructiva y debe usarse para correcciones de planificación.
     *
     * URL de acceso: {@code DELETE http://localhost:8080/api/asignaciones/{id}}
     *
     * @param id Identificador único de la asignación a eliminar.
     * @return ResponseEntity con cuerpo vacío y estado 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable("id") Long id) {
        asignacionService.eliminarAsignacion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Proporciona el listado de modalidades de turno disponibles (Presencial, Teletrabajo, etc.).
     * Sirve como endpoint de diccionario para la interfaz de usuario.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/asignaciones/modalidades}
     *
     * @return ResponseEntity con una lista de los valores del enum {@link ModalidadTurno}.
     */
    @GetMapping("/modalidades")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<ModalidadTurno>> obtenerModalidades() {
        return ResponseEntity.ok(Arrays.asList(ModalidadTurno.values()));
    }
}
