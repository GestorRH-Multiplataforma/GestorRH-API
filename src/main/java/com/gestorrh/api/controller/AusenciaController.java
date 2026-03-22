package com.gestorrh.api.controller;

import com.gestorrh.api.dto.ausencia.PeticionAusenciaDTO;
import com.gestorrh.api.dto.ausencia.PeticionRevisionAusenciaDTO;
import com.gestorrh.api.dto.ausencia.RespuestaAusenciaDTO;
import com.gestorrh.api.entity.enums.EstadoAusencia;
import com.gestorrh.api.entity.enums.TipoAusencia;
import com.gestorrh.api.service.AusenciaService;
import com.gestorrh.api.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Controlador REST para la gestión integral del ciclo de vida de las Ausencias.
 * Permite a los empleados solicitar ausencias y adjuntar justificantes, y a las
 * empresas o supervisores revisar, aprobar o denegar dichas solicitudes.
 */
@RestController
@RequestMapping("/api/ausencias")
@RequiredArgsConstructor
public class AusenciaController {

    private final AusenciaService ausenciaService;
    private final FileStorageService fileStorageService;

    /**
     * Obtiene el listado de todos los tipos de ausencia configurados en el sistema.
     * Sirve como endpoint de consulta para cargar opciones en formularios de solicitud.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/ausencias/tipos}
     *
     * @return ResponseEntity con una lista de los valores del enum {@link TipoAusencia}.
     */
    @GetMapping("/tipos")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<TipoAusencia>> obtenerTiposAusencia() {
        return ResponseEntity.ok(Arrays.asList(TipoAusencia.values()));
    }

    /**
     * Obtiene el listado de los posibles estados en los que puede encontrarse una ausencia.
     * Facilita el filtrado de solicitudes en las interfaces de usuario.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/ausencias/estados}
     *
     * @return ResponseEntity con una lista de los valores del enum {@link EstadoAusencia}.
     */
    @GetMapping("/estados")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<EstadoAusencia>> obtenerEstadosAusencia() {
        return ResponseEntity.ok(Arrays.asList(EstadoAusencia.values()));
    }

    /**
     * Permite a un empleado crear una nueva solicitud de ausencia.
     * El endpoint soporta el envío de datos en formato JSON y un archivo (justificante) opcional.
     *
     * URL de acceso: {@code POST http://localhost:8080/api/ausencias}
     *
     * @param peticion DTO con los detalles de la ausencia (fechas, tipo, motivo).
     * @param archivo Archivo opcional que sirve como justificante documental de la ausencia.
     * @return ResponseEntity con el {@link RespuestaAusenciaDTO} creado y estado 201 (Created).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<RespuestaAusenciaDTO> crearAusencia(
            @RequestPart("datos") @Valid PeticionAusenciaDTO peticion,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        RespuestaAusenciaDTO respuesta = ausenciaService.crearAusencia(peticion, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Recupera el historial de ausencias solicitadas por el empleado que realiza la petición.
     * Permite el filtrado opcional por el estado de la solicitud.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/ausencias/me}
     *
     * @param estado Estado opcional por el cual filtrar las ausencias del empleado.
     * @return ResponseEntity con la lista de {@link RespuestaAusenciaDTO} del empleado autenticado.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<List<RespuestaAusenciaDTO>> obtenerMisAusencias(
            @RequestParam(value = "estado", required = false) EstadoAusencia estado) {
        List<RespuestaAusenciaDTO> lista = ausenciaService.obtenerMisAusencias(estado);
        return ResponseEntity.ok(lista);
    }

    /**
     * Permite a un empleado actualizar los datos o el justificante de una ausencia ya solicitada.
     * Únicamente se permiten cambios si la ausencia se encuentra en un estado que permita edición.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/ausencias/{id}}
     *
     * @param id Identificador único de la ausencia a modificar.
     * @param peticion DTO con los nuevos datos de la ausencia.
     * @param archivo Nuevo archivo justificante opcional que reemplazará al anterior si se proporciona.
     * @return ResponseEntity con el {@link RespuestaAusenciaDTO} actualizado.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<RespuestaAusenciaDTO> actualizarMiAusencia(
            @PathVariable("id") Long id,
            @RequestPart("datos") @Valid PeticionAusenciaDTO peticion,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        RespuestaAusenciaDTO respuesta = ausenciaService.actualizarMiAusencia(id, peticion, archivo);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Elimina de forma permanente una solicitud de ausencia realizada por el empleado.
     * Solo es posible eliminar ausencias que aún no hayan sido procesadas definitivamente.
     *
     * URL de acceso: {@code DELETE http://localhost:8080/api/ausencias/{id}}
     *
     * @param id Identificador único de la ausencia a eliminar.
     * @return ResponseEntity con cuerpo vacío y estado 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<Void> eliminarMiAusencia(@PathVariable("id") Long id) {
        ausenciaService.eliminarMiAusencia(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lista todas las solicitudes de ausencia que el usuario autenticado tiene permiso para visualizar.
     * Las empresas ven todas las de sus empleados, mientras que los supervisores ven las de su departamento.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/ausencias}
     *
     * @param estado Estado opcional para filtrar el listado de ausencias a revisar.
     * @return ResponseEntity con la lista de {@link RespuestaAusenciaDTO} permitidas.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<RespuestaAusenciaDTO>> listarAusenciasPermitidas(
            @RequestParam(value = "estado", required = false) EstadoAusencia estado) {
        List<RespuestaAusenciaDTO> lista = ausenciaService.obtenerAusenciasPermitidas(estado);
        return ResponseEntity.ok(lista);
    }

    /**
     * Procesa la revisión de una solicitud de ausencia, permitiendo su aprobación o denegación.
     * Se debe incluir un comentario de revisión que justifique la decisión tomada.
     *
     * URL de acceso: {@code PUT http://localhost:8080/api/ausencias/{id}/revision}
     *
     * @param id Identificador único de la ausencia a revisar.
     * @param peticion DTO con el nuevo estado (aprobado/denegado) y el comentario de revisión.
     * @return ResponseEntity con el {@link RespuestaAusenciaDTO} actualizado tras la revisión.
     */
    @PutMapping("/{id}/revision")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<RespuestaAusenciaDTO> revisarAusencia(
            @PathVariable("id") Long id,
            @Valid @RequestBody PeticionRevisionAusenciaDTO peticion) {
        RespuestaAusenciaDTO respuesta = ausenciaService.revisarAusencia(id, peticion);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * Permite la descarga del archivo justificante asociado a una solicitud de ausencia.
     * El archivo se retorna como un recurso descargable con las cabeceras HTTP apropiadas.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/ausencias/justificantes/{nombreArchivo}}
     *
     * @param nombreArchivo Nombre del archivo almacenado en el sistema que se desea descargar.
     * @return ResponseEntity que contiene el archivo como {@link Resource} y las cabeceras de descarga.
     */
    @GetMapping("/justificantes/{nombreArchivo}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    public ResponseEntity<Resource> descargarJustificante(@PathVariable String nombreArchivo) {
        Resource resource = fileStorageService.cargarArchivoComoRecurso(nombreArchivo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
