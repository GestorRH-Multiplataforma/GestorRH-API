package com.gestorrh.api.controller;

import com.gestorrh.api.dto.reporte.ReporteDetalleDTO;
import com.gestorrh.api.dto.reporte.ReporteResumenDTO;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import com.gestorrh.api.service.ReportePdfService;
import com.gestorrh.api.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador REST dedicado a la generación y descarga de reportes de asistencia y fichajes.
 * Proporciona endpoints para obtener datos detallados o resumidos, tanto en formato JSON
 * como en documentos PDF descargables, facilitando la auditoría y el control de horas trabajadas.
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;
    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;

    /**
     * Obtiene el historial pormenorizado de fichajes y cálculos de horas realizados en un rango de fechas.
     * Este endpoint es idóneo para ser utilizado como fuente de datos en informes detallados,
     * permitiendo filtrar opcionalmente por un empleado específico.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/detalle?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     *
     * @param fechaInicio Fecha inicial del periodo a consultar (formato ISO: YYYY-MM-DD).
     * @param fechaFin Fecha final del periodo a consultar (formato ISO: YYYY-MM-DD).
     * @param idEmpleado Identificador opcional del empleado para filtrar los resultados.
     * @return ResponseEntity con una lista de {@link ReporteDetalleDTO} y estado HTTP 200 (OK).
     */
    @GetMapping("/detalle")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    public ResponseEntity<List<ReporteDetalleDTO>> generarReporteDetallado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idEmpleado) {

        List<ReporteDetalleDTO> reporte = reporteService.obtenerReporteDetallado(fechaInicio, fechaFin, idEmpleado);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtiene un resumen consolidado de las horas totales y extras trabajadas por los empleados en un periodo.
     * Los datos se agrupan por empleado, siendo ideal para visualizaciones en cuadros de mando
     * y revisiones ejecutivas de productividad.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/resumen?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     *
     * @param fechaInicio Fecha inicial del periodo a resumir (formato ISO: YYYY-MM-DD).
     * @param fechaFin Fecha final del periodo a resumir (formato ISO: YYYY-MM-DD).
     * @param idEmpleado Identificador opcional del empleado para filtrar el resumen.
     * @return ResponseEntity con una lista de {@link ReporteResumenDTO} y estado HTTP 200 (OK).
     */
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    public ResponseEntity<List<ReporteResumenDTO>> generarReporteResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idEmpleado) {

        List<ReporteResumenDTO> reporte = reporteService.obtenerReporteResumen(fechaInicio, fechaFin, idEmpleado);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Genera y permite la descarga de un documento PDF con el reporte detallado de fichajes.
     * El documento incluye el nombre de la empresa autenticada y el rango de fechas seleccionado.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/detalle/pdf?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     *
     * @param fechaInicio Fecha de inicio del reporte.
     * @param fechaFin Fecha de fin del reporte.
     * @param idEmpleado Identificador opcional del empleado para el cual se genera el reporte.
     * @return ResponseEntity con el contenido binario del PDF, cabeceras de tipo de contenido y disposición de archivo.
     */
    @GetMapping("/detalle/pdf")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    public ResponseEntity<byte[]> descargarPdfDetalle(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "idEmpleado", required = false) Long idEmpleado) {

        List<ReporteDetalleDTO> datos = reporteService.obtenerReporteDetallado(fechaInicio, fechaFin, idEmpleado);

        String nombreEmpresa = obtenerNombreEmpresaAutenticada();
        String subtitulo = "Del " + fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " al " + fechaFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        byte[] pdfBytes = reportePdfService.generarPdfDetalle(nombreEmpresa, subtitulo, datos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Reporte_Detalle_" + fechaInicio + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /**
     * Genera y permite la descarga de un documento PDF con el resumen de horas por empleado.
     * Facilita una visión rápida y profesional de la distribución de horas en un intervalo temporal.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/resumen/pdf?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     *
     * @param fechaInicio Fecha de inicio del resumen.
     * @param fechaFin Fecha de fin del resumen.
     * @param idEmpleado Identificador opcional del empleado para el cual se genera el resumen.
     * @return ResponseEntity con el contenido binario del PDF y cabeceras adecuadas para su descarga.
     */
    @GetMapping("/resumen/pdf")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    public ResponseEntity<byte[]> descargarPdfResumen(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "idEmpleado", required = false) Long idEmpleado) {

        List<ReporteResumenDTO> datos = reporteService.obtenerReporteResumen(fechaInicio, fechaFin, idEmpleado);

        String nombreEmpresa = obtenerNombreEmpresaAutenticada();
        String subtitulo = "Del " + fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " al " + fechaFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        byte[] pdfBytes = reportePdfService.generarPdfResumen(nombreEmpresa, subtitulo, datos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Reporte_Resumen_" + fechaInicio + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }


    /**
     * Obtiene el nombre de la empresa asociada al usuario que ha realizado la petición.
     * Determina si el usuario es una Empresa o un Empleado y recupera el nombre correspondiente
     * desde el repositorio adecuado.
     *
     * @return El nombre de la empresa como cadena de texto.
     */
    private String obtenerNombreEmpresaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));

        if (esEmpresa) {
            return empresaRepository.findByEmail(email).map(Empresa::getNombre).orElse("EMPRESA");
        } else {
            return empleadoRepository.findByEmail(email).map(e -> e.getEmpresa().getNombre()).orElse("EMPRESA");
        }
    }
}
