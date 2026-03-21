package com.gestorrh.api.controller;

import com.gestorrh.api.dto.reporteDTO.ReporteDetalleDTO;
import com.gestorrh.api.dto.reporteDTO.ReporteResumenDTO;
import com.gestorrh.api.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /**
     * Obtiene el historial fila a fila de los fichajes y sus cálculos de horas.
     * Ideal para inyectar como origen de datos en el detalle de JasperReports.
     * * EJEMPLO DE URL:
     * URL: http://localhost:8080/api/reportes/detalle?fechaInicio=2026-03-01&fechaFin=2026-03-31
     * URL: http://localhost:8080/api/reportes/detalle?fechaInicio=2026-03-01&fechaFin=2026-03-31&idEmpleado=5
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
     * Obtiene un resumen matemático agrupado por empleado (Suma de horas totales y extras).
     * Ideal para PDFs ejecutivos de reuniones y gráficos estadísticos (Dashboards).
     *
     * URL: http://localhost:8080/api/reportes/resumen?fechaInicio=2026-03-01&fechaFin=2026-03-31
     * URL: http://localhost:8080/api/reportes/resumen?fechaInicio=2026-03-01&fechaFin=2026-03-31&idEmpleado=5
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
}
