package com.gestorrh.api.controller;

import com.gestorrh.api.dto.estadisticas.DatoGraficoDTO;
import com.gestorrh.api.service.EstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    /**
     * Devuelve las 3 métricas principales en un solo JSON:
     * - totalEmpleados
     * - planificadosHoy
     * - ausentesHoy
     * URL: http://localhost:8080/api/estadisticas/kpis
     */
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Long>> obtenerKpisDashboard() {
        return ResponseEntity.ok(estadisticasService.obtenerKpisDashboard());
    }

    /**
     * Ideal para: Gráfico de Barras o de Sectores (PieChart)
     * URL: http://localhost:8080/api/estadisticas/empleados-departamento
     */
    @GetMapping("/empleados-departamento")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerEmpleadosPorDepartamento() {
        return ResponseEntity.ok(estadisticasService.obtenerEmpleadosPorDepartamento());
    }

    /**
     * Ideal para: Gráfico de Donut o Sectores
     * URL: http://localhost:8080/api/estadisticas/ausencias-tipo
     */
    @GetMapping("/ausencias-tipo")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerAusenciasPorTipo() {
        return ResponseEntity.ok(estadisticasService.obtenerAusenciasPorTipo());
    }

    /**
     * Ideal para: Gráfico de Sectores
     * URL: http://localhost:8080/api/estadisticas/ausencias-estado
     */
    @GetMapping("/ausencias-estado")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerAusenciasPorEstado() {
        return ResponseEntity.ok(estadisticasService.obtenerAusenciasPorEstado());
    }

    /**
     * Ideal para: Gráfico de Barras horizontales (Ranking)
     * URL: http://localhost:8080/api/estadisticas/top-retrasos
     */
    @GetMapping("/top-retrasos")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerTopRetrasos() {
        return ResponseEntity.ok(estadisticasService.obtenerTopRetrasos());
    }
}
