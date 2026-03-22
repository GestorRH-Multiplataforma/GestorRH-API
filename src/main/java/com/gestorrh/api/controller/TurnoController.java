package com.gestorrh.api.controller;

import com.gestorrh.api.dto.turno.PeticionTurnoDTO;
import com.gestorrh.api.dto.turno.RespuestaTurnoDTO;
import com.gestorrh.api.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión del catálogo de Turnos.
 * Proporciona endpoints para crear, listar, actualizar y eliminar los diferentes turnos de trabajo
 * que una empresa puede asignar a sus empleados.
 */
@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    /**
     * Endpoint para la creación de un nuevo tipo de turno en el sistema.
     * Este recurso es de acceso restringido únicamente para usuarios que posean el rol de 'EMPRESA'.
     *
     * URL de acceso: {@code POST http://localhost:8080/api/turnos}
     *
     * @param peticion DTO que contiene los detalles del turno a crear, incluyendo descripción, hora de inicio y hora de fin.
     * @return ResponseEntity con el objeto {@link RespuestaTurnoDTO} que representa el turno recién creado y el código de estado HTTP 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<RespuestaTurnoDTO> crearTurno(@Valid @RequestBody PeticionTurnoDTO peticion) {
        RespuestaTurnoDTO respuesta = turnoService.crearTurno(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Endpoint para obtener el listado completo de todos los turnos configurados por la empresa autenticada.
     * Este recurso es accesible para usuarios con los roles 'EMPRESA' o 'SUPERVISOR'.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/turnos}
     *
     * @return ResponseEntity que contiene una lista de objetos {@link RespuestaTurnoDTO} y el código de estado HTTP 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<RespuestaTurnoDTO>> listarTurnos() {
        List<RespuestaTurnoDTO> lista = turnoService.obtenerTurnosDeEmpresa();
        return ResponseEntity.ok(lista);
    }

    /**
     * Endpoint para modificar la información de un turno ya existente en la base de datos.
     * Este recurso es de acceso restringido únicamente para usuarios con el rol de 'EMPRESA'.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/turnos/{id}}
     *
     * @param id Identificador único y numérico del turno que se desea modificar.
     * @param peticion DTO que contiene los nuevos datos que se asignarán al turno especificado.
     * @return ResponseEntity con el objeto {@link RespuestaTurnoDTO} actualizado y el código de estado HTTP 200 (OK).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<RespuestaTurnoDTO> actualizarTurno(
            @PathVariable("id") Long id,
            @Valid @RequestBody PeticionTurnoDTO peticion) {
        RespuestaTurnoDTO respuesta = turnoService.actualizarTurno(id, peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Endpoint para proceder a la eliminación física de un turno del catálogo de la empresa.
     * Este recurso es de acceso restringido únicamente para usuarios con el rol de 'EMPRESA'.
     *
     * URL de acceso: {@code DELETE http://localhost:8080/api/turnos/{id}}
     *
     * @param id Identificador único y numérico del turno que se desea eliminar de forma permanente.
     * @return ResponseEntity con cuerpo vacío y el código de estado HTTP 204 (No Content) tras una ejecución exitosa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<Void> eliminarTurno(@PathVariable("id") Long id) {
        turnoService.eliminarTurno(id);
        return ResponseEntity.noContent().build();
    }
}
