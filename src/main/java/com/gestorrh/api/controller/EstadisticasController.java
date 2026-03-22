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

/**
 * Controlador REST para la consulta de métricas y estadísticas operativas del sistema.
 * Proporciona datos procesados para su visualización en gráficos y tableros de control (dashboards),
 * permitiendo una toma de decisiones informada por parte de la empresa y supervisores.
 */
@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    /**
     * Obtiene los Indicadores Clave de Desempeño (KPIs) generales para el cuadro de mando.
     * Incluye el conteo total de empleados, los planificados para la jornada actual y los ausentes.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/kpis}
     *
     * @return ResponseEntity que contiene un mapa con los nombres de las métricas y sus respectivos valores numéricos.
     */
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Long>> obtenerKpisDashboard() {
        return ResponseEntity.ok(estadisticasService.obtenerKpisDashboard());
    }

    /**
     * Obtiene la distribución del número de empleados por cada departamento de la empresa.
     * Información óptima para su representación en gráficos de barras o de sectores.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/empleados-departamento}
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando los departamentos y su cantidad de empleados.
     */
    @GetMapping("/empleados-departamento")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerEmpleadosPorDepartamento() {
        return ResponseEntity.ok(estadisticasService.obtenerEmpleadosPorDepartamento());
    }

    /**
     * Obtiene estadísticas sobre las ausencias aprobadas, agrupadas por su tipología (médica, vacaciones, etc.).
     * Información óptima para su representación en gráficos de donut o de sectores.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/ausencias-tipo}
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando los tipos de ausencia y su frecuencia.
     */
    @GetMapping("/ausencias-tipo")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerAusenciasAprobadasPorTipo() {
        return ResponseEntity.ok(estadisticasService.obtenerAusenciasAprobadasPorTipo());
    }

    /**
     * Obtiene estadísticas sobre el estado de las solicitudes de ausencia (pendientes, aprobadas, denegadas).
     * Permite visualizar la carga de trabajo administrativo en la revisión de solicitudes.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/ausencias-estado}
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando los estados y su conteo.
     */
    @GetMapping("/ausencias-estado")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerAusenciasPorEstado() {
        return ResponseEntity.ok(estadisticasService.obtenerAusenciasPorEstado());
    }

    /**
     * Obtiene un listado de los empleados con mayor número de retrasos acumulados en sus fichajes.
     * Información óptima para identificar desviaciones recurrentes en la puntualidad.
     *
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/top-retrasos}
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando el ranking de retrasos por empleado.
     */
    @GetMapping("/top-retrasos")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    public ResponseEntity<List<DatoGraficoDTO>> obtenerTopRetrasos() {
        return ResponseEntity.ok(estadisticasService.obtenerTopRetrasos());
    }
}
