package com.gestorrh.api.controller;

import com.gestorrh.api.annotation.ApiErroresLectura;
import com.gestorrh.api.dto.reporte.ReporteDetalleDTO;
import com.gestorrh.api.dto.reporte.ReporteResumenDTO;
import com.gestorrh.api.service.ReportePdfService;
import com.gestorrh.api.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador REST dedicado a la generación y descarga de reportes de asistencia y fichajes.
 * <p>
 * Proporciona endpoints para obtener datos detallados o resumidos, tanto en formato JSON
 * como en documentos PDF descargables, facilitando la auditoría y el control de horas trabajadas.
 * </p>
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(
        name = "9. Reportes",
        description = "Generación de informes de horas trabajadas y fichajes. Formatos disponibles: JSON (para tablas Web) y PDF descargables."
)
public class ReporteController {

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;

    /**
     * Obtiene el historial pormenorizado de fichajes y cálculos de horas realizados en un rango de fechas.
     * <p>
     * Este endpoint es idóneo para ser utilizado como fuente de datos en informes detallados,
     * permitiendo filtrar opcionalmente por un empleado específico.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/detalle?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     * </p>
     *
     * @param fechaInicio Fecha inicial del periodo a consultar (formato ISO: YYYY-MM-DD).
     * @param fechaFin Fecha final del periodo a consultar (formato ISO: YYYY-MM-DD).
     * @param idEmpleado Identificador opcional del empleado para filtrar los resultados.
     * @return ResponseEntity con una lista de {@link ReporteDetalleDTO} y estado HTTP 200 (OK).
     */
    @GetMapping("/detalle")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    @Operation(
            summary = "Obtener reporte detallado (JSON)",
            description = "Requiere Token de EMPRESA, SUPERVISOR o EMPLEADO. Devuelve el desglose diario de fichajes. " +
                    "Los empleados solo ven sus datos; Empresa/Supervisor pueden filtrar usando el parámetro opcional 'idEmpleado'."
    )
    @ApiErroresLectura
    public ResponseEntity<List<ReporteDetalleDTO>> generarReporteDetallado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idEmpleado) {

        List<ReporteDetalleDTO> reporte = reporteService.obtenerReporteDetallado(fechaInicio, fechaFin, idEmpleado);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtiene un resumen consolidado de las horas totales y extras trabajadas por los empleados en un periodo.
     * <p>
     * Los datos se agrupan por empleado, siendo ideal para visualizaciones en cuadros de mando
     * y revisiones ejecutivas de productividad.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/resumen?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     * </p>
     *
     * @param fechaInicio Fecha inicial del periodo a resumir (formato ISO: YYYY-MM-DD).
     * @param fechaFin Fecha final del periodo a resumir (formato ISO: YYYY-MM-DD).
     * @param idEmpleado Identificador opcional del empleado para filtrar el resumen.
     * @return ResponseEntity con una lista de {@link ReporteResumenDTO} y estado HTTP 200 (OK).
     */
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    @Operation(
            summary = "Obtener reporte resumido (JSON)",
            description = "Requiere Token de EMPRESA, SUPERVISOR o EMPLEADO. Devuelve el total consolidado de horas " +
                    "trabajadas y extras en el periodo. Los empleados solo ven sus datos; Empresa/Supervisor pueden filtrar " +
                    "usando el parámetro opcional 'idEmpleado'."
    )
    @ApiErroresLectura
    public ResponseEntity<List<ReporteResumenDTO>> generarReporteResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idEmpleado) {

        List<ReporteResumenDTO> reporte = reporteService.obtenerReporteResumen(fechaInicio, fechaFin, idEmpleado);
        return ResponseEntity.ok(reporte);
    }

    /**
     * Genera y permite la descarga de un documento PDF con el reporte detallado de fichajes.
     * <p>
     * El documento incluye el nombre de la empresa autenticada y el rango de fechas seleccionado.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/detalle/pdf?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     * </p>
     *
     * @param fechaInicio Fecha de inicio del reporte.
     * @param fechaFin Fecha de fin del reporte.
     * @param idEmpleado Identificador opcional del empleado para el cual se genera el reporte.
     * @return ResponseEntity con el contenido binario del PDF, cabeceras de tipo de contenido y disposición de archivo.
     */
    @GetMapping("/detalle/pdf")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    @Operation(
            summary = "Descargar reporte detallado (PDF)",
            description = "Requiere Token de EMPRESA, SUPERVISOR o EMPLEADO. Genera y descarga un documento PDF oficial " +
                    "con el desglose diario de fichajes."
    )
    @ApiErroresLectura
    public ResponseEntity<byte[]> descargarPdfDetalle(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "idEmpleado", required = false) Long idEmpleado) {

        List<ReporteDetalleDTO> datos = reporteService.obtenerReporteDetallado(fechaInicio, fechaFin, idEmpleado);

        String nombreEmpresa = reporteService.obtenerNombreEmpresaAutenticada();
        String subtitulo = construirSubtituloFechas(fechaInicio, fechaFin);

        byte[] pdfBytes = reportePdfService.generarPdfDetalle(nombreEmpresa, subtitulo, datos);

        return construirRespuestaPdf(pdfBytes, "Reporte_Detalle_" + fechaInicio + ".pdf");
    }

    /**
     * Genera y permite la descarga de un documento PDF con el resumen de horas por empleado.
     * <p>
     * Facilita una visión rápida y profesional de la distribución de horas en un intervalo temporal.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/reportes/resumen/pdf?fechaInicio=2026-03-01&fechaFin=2026-03-31}
     * </p>
     *
     * @param fechaInicio Fecha de inicio del resumen.
     * @param fechaFin Fecha de fin del resumen.
     * @param idEmpleado Identificador opcional del empleado para el cual se genera el resumen.
     * @return ResponseEntity con el contenido binario del PDF y cabeceras adecuadas para su descarga.
     */
    @GetMapping("/resumen/pdf")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR', 'EMPLEADO')")
    @Operation(
            summary = "Descargar reporte resumido (PDF)",
            description = "Requiere Token de EMPRESA, SUPERVISOR o EMPLEADO. Genera y descarga un documento PDF oficial " +
                    "con el total consolidado de horas por empleado."
    )
    @ApiErroresLectura
    public ResponseEntity<byte[]> descargarPdfResumen(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "idEmpleado", required = false) Long idEmpleado) {

        List<ReporteResumenDTO> datos = reporteService.obtenerReporteResumen(fechaInicio, fechaFin, idEmpleado);

        String nombreEmpresa = reporteService.obtenerNombreEmpresaAutenticada();
        String subtitulo = construirSubtituloFechas(fechaInicio, fechaFin);

        byte[] pdfBytes = reportePdfService.generarPdfResumen(nombreEmpresa, subtitulo, datos);

        return construirRespuestaPdf(pdfBytes, "Reporte_Resumen_" + fechaInicio + ".pdf");
    }

    /**
     * Construye la cadena de subtítulo con el rango de fechas formateado para los documentos PDF.
     *
     * @param fechaInicio Fecha de inicio del rango.
     * @param fechaFin Fecha de fin del rango.
     * @return Cadena con el formato "Del dd/MM/yyyy al dd/MM/yyyy".
     */
    private String construirSubtituloFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "Del " + fechaInicio.format(formatter) + " al " + fechaFin.format(formatter);
    }

    /**
     * Construye la respuesta HTTP estándar para la descarga de un archivo PDF.
     *
     * @param pdfBytes Contenido binario del documento PDF generado.
     * @param nombreArchivo Nombre del archivo que se propondrá en la descarga.
     * @return ResponseEntity con el PDF, las cabeceras de tipo de contenido y disposición de archivo.
     */
    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] pdfBytes, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
