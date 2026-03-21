package com.gestorrh.api.controller;

import com.gestorrh.api.dto.ausenciaDTO.PeticionAusenciaDTO;
import com.gestorrh.api.dto.ausenciaDTO.PeticionRevisionAusenciaDTO;
import com.gestorrh.api.dto.ausenciaDTO.RespuestaAusenciaDTO;
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
 * Controlador REST para la gestión de Ausencias (Épica E6).
 */
@RestController
@RequestMapping("/api/ausencias")
@RequiredArgsConstructor
public class AusenciaController {

    private final AusenciaService ausenciaService;
    private final FileStorageService fileStorageService;

    // ENDPOINTS DE DICCIONARIO

    @GetMapping("/tipos")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<TipoAusencia>> obtenerTiposAusencia() {
        return ResponseEntity.ok(Arrays.asList(TipoAusencia.values()));
    }

    @GetMapping("/estados")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<EstadoAusencia>> obtenerEstadosAusencia() {
        return ResponseEntity.ok(Arrays.asList(EstadoAusencia.values()));
    }

    // ENDPOINTS DEL EMPLEADO

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<RespuestaAusenciaDTO> crearAusencia(
            @RequestPart("datos") @Valid PeticionAusenciaDTO peticion,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        RespuestaAusenciaDTO respuesta = ausenciaService.crearAusencia(peticion, archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<List<RespuestaAusenciaDTO>> obtenerMisAusencias(
            @RequestParam(value = "estado", required = false) EstadoAusencia estado) {
        List<RespuestaAusenciaDTO> lista = ausenciaService.obtenerMisAusencias(estado);
        return ResponseEntity.ok(lista);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<RespuestaAusenciaDTO> actualizarMiAusencia(
            @PathVariable("id") Long id,
            @RequestPart("datos") @Valid PeticionAusenciaDTO peticion,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        RespuestaAusenciaDTO respuesta = ausenciaService.actualizarMiAusencia(id, peticion, archivo);
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLEADO')")
    public ResponseEntity<Void> eliminarMiAusencia(@PathVariable("id") Long id) {
        ausenciaService.eliminarMiAusencia(id);
        return ResponseEntity.noContent().build();
    }

    // ENDPOINTS DE REVISIÓN

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<List<RespuestaAusenciaDTO>> listarAusenciasPermitidas(
            @RequestParam(value = "estado", required = false) EstadoAusencia estado) {
        List<RespuestaAusenciaDTO> lista = ausenciaService.obtenerAusenciasPermitidas(estado);
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}/revision")
    @PreAuthorize("hasAnyRole('EMPRESA', 'EMPLEADO')")
    public ResponseEntity<RespuestaAusenciaDTO> revisarAusencia(
            @PathVariable("id") Long id,
            @Valid @RequestBody PeticionRevisionAusenciaDTO peticion) {
        RespuestaAusenciaDTO respuesta = ausenciaService.revisarAusencia(id, peticion);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/justificantes/{nombreArchivo}")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    public ResponseEntity<Resource> descargarJustificante(@PathVariable String nombreArchivo) {
        Resource resource = fileStorageService.cargarArchivoComoRecurso(nombreArchivo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
