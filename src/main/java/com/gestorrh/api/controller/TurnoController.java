package com.gestorrh.api.controller;

import com.gestorrh.api.annotation.ApiErroresAccion;
import com.gestorrh.api.annotation.ApiErroresEscritura;
import com.gestorrh.api.annotation.ApiErroresLectura;
import com.gestorrh.api.dto.turno.PeticionTurnoDTO;
import com.gestorrh.api.dto.turno.RespuestaTurnoDTO;
import com.gestorrh.api.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión del catálogo de Turnos.
 * <p>
 * Proporciona endpoints para crear, listar, actualizar y eliminar los diferentes turnos de trabajo
 * que una empresa puede asignar a sus empleados.
 * </p>
 */
@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(
        name = "4. Turnos",
        description = "Gestión del catálogo de turnos base (plantillas de horarios). " +
                "Nota: La EMPRESA tiene control total, mientras que el SUPERVISOR solo tiene permisos de lectura."
)
public class TurnoController {

    private final TurnoService turnoService;

    /**
     * Endpoint para la creación de un nuevo tipo de turno en el sistema.
     * <p>
     * Este recurso es de acceso restringido únicamente para usuarios que posean el rol de 'EMPRESA'.
     * </p>
     * <p>
     * URL de acceso: {@code POST http://localhost:8080/api/turnos}
     * </p>
     *
     * @param peticion DTO que contiene los detalles del turno a crear, incluyendo descripción, hora de inicio y hora de fin.
     * @return ResponseEntity con el objeto {@link RespuestaTurnoDTO} que representa el turno recién creado y el código de estado HTTP 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(
            summary = "Crear un nuevo turno",
            description = "Requiere Token de EMPRESA. Añade un nuevo tipo de turno (plantilla con hora de inicio y fin) al catálogo de la empresa.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Turno creado con éxito")
            }
    )
    @ApiErroresEscritura
    public ResponseEntity<RespuestaTurnoDTO> crearTurno(@Valid @RequestBody PeticionTurnoDTO peticion) {
        RespuestaTurnoDTO respuesta = turnoService.crearTurno(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Endpoint para obtener el listado completo de todos los turnos configurados por la empresa autenticada.
     * <p>
     * Este recurso es accesible para usuarios con los roles 'EMPRESA' o 'SUPERVISOR'.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/turnos}
     * </p>
     *
     * @return ResponseEntity que contiene una lista de objetos {@link RespuestaTurnoDTO} y el código de estado HTTP 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    @Operation(
            summary = "Listar turnos de la empresa",
            description = "Requiere Token de EMPRESA o SUPERVISOR. Obtiene el catálogo completo de todos los turnos configurados por la empresa."
    )
    @ApiErroresLectura
    public ResponseEntity<List<RespuestaTurnoDTO>> listarTurnos() {
        List<RespuestaTurnoDTO> lista = turnoService.obtenerTurnosDeEmpresa();
        return ResponseEntity.ok(lista);
    }

    /**
     * Endpoint para modificar la información de un turno ya existente en la base de datos.
     * <p>
     * Este recurso es de acceso restringido únicamente para usuarios con el rol de 'EMPRESA'.
     * </p>
     * <p>
     * URL de acceso: {@code PUT http://localhost:8080/api/turnos/{id}}
     * </p>
     *
     * @param id Identificador único y numérico del turno que se desea modificar.
     * @param peticion DTO que contiene los nuevos datos que se asignarán al turno especificado.
     * @return ResponseEntity con el objeto {@link RespuestaTurnoDTO} actualizado y el código de estado HTTP 200 (OK).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(
            summary = "Actualizar un turno",
            description = "Requiere Token de EMPRESA. Modifica la información (descripción, hora inicio/fin) de un turno existente en el catálogo."
    )
    @ApiErroresAccion
    public ResponseEntity<RespuestaTurnoDTO> actualizarTurno(
            @PathVariable("id") Long id,
            @Valid @RequestBody PeticionTurnoDTO peticion) {
        RespuestaTurnoDTO respuesta = turnoService.actualizarTurno(id, peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Endpoint para proceder a la eliminación física de un turno del catálogo de la empresa.
     * <p>
     * Este recurso es de acceso restringido únicamente para usuarios con el rol de 'EMPRESA'.
     * </p>
     * <p>
     * URL de acceso: {@code DELETE http://localhost:8080/api/turnos/{id}}
     * </p>
     *
     * @param id Identificador único y numérico del turno que se desea eliminar de forma permanente.
     * @return ResponseEntity con cuerpo vacío y el código de estado HTTP 204 (No Content) tras una ejecución exitosa.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    @Operation(
            summary = "Eliminar un turno",
            description = "Requiere Token de EMPRESA. Borra físicamente un turno del catálogo de la empresa. Acción irreversible.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Turno eliminado con éxito")
            }
    )
    @ApiErroresAccion
    public ResponseEntity<Void> eliminarTurno(@PathVariable("id") Long id) {
        turnoService.eliminarTurno(id);
        return ResponseEntity.noContent().build();
    }
}
