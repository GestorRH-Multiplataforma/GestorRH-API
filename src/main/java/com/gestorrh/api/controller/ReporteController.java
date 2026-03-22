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

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;
    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;

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

    // MÉTODOS PRIVADOS

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
