package com.gestorrh.api.controller;

import com.gestorrh.api.annotation.ApiErroresLectura;
import com.gestorrh.api.dto.estadisticas.DatoGraficoDTO;
import com.gestorrh.api.service.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * <p>
 * Proporciona datos procesados para su visualización en gráficos y tableros de control (dashboards),
 * permitiendo una toma de decisiones informada por parte de la empresa y supervisores.
 * </p>
 */
@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
@Tag(
        name = "8. Estadísticas",
        description = "Métricas y cuadros de mando (Dashboards). Acceso exclusivo para perfiles con capacidad de gestión (EMPRESA y SUPERVISOR)."
)
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    /**
     * Obtiene los Indicadores Clave de Desempeño (KPIs) generales para el cuadro de mando.
     * <p>
     * Incluye el conteo total de empleados, los planificados para la jornada actual y los ausentes.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/kpis}
     * </p>
     *
     * @return ResponseEntity que contiene un mapa con los nombres de las métricas y sus respectivos valores numéricos.
     */
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    @Operation(
            summary = "Obtener KPIs generales",
            description = "Requiere Token de EMPRESA o SUPERVISOR. Devuelve los indicadores clave numéricos " +
                    "(total empleados, planificados de hoy, ausentes) para la cabecera del Dashboard."
    )
    @ApiErroresLectura
    public ResponseEntity<Map<String, Long>> obtenerKpisDashboard() {
        return ResponseEntity.ok(estadisticasService.obtenerKpisDashboard());
    }

    /**
     * Obtiene la distribución del número de empleados por cada departamento de la empresa.
     * <p>
     * Información óptima para su representación en gráficos de barras o de sectores.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/empleados-departamento}
     * </p>
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando los departamentos y su cantidad de empleados.
     */
    @GetMapping("/empleados-departamento")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    @Operation(
            summary = "Empleados por departamento",
            description = "Requiere Token de EMPRESA o SUPERVISOR. Devuelve la distribución de la plantilla estructurada " +
                    "para pintar gráficos de sectores o barras."
    )
    @ApiErroresLectura
    public ResponseEntity<List<DatoGraficoDTO>> obtenerEmpleadosPorDepartamento() {
        return ResponseEntity.ok(estadisticasService.obtenerEmpleadosPorDepartamento());
    }

    /**
     * Obtiene estadísticas sobre las ausencias aprobadas, agrupadas por su tipología (médica, vacaciones, etc.).
     * <p>
     * Información óptima para su representación en gráficos de donut o de sectores.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/ausencias-tipo}
     * </p>
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando los tipos de ausencia y su frecuencia.
     */
    @GetMapping("/ausencias-tipo")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    @Operation(
            summary = "Ausencias aprobadas por tipo",
            description = "Requiere Token de EMPRESA o SUPERVISOR. Agrupa las ausencias aprobadas según su tipología " +
                    "(ej: Vacaciones, Baja Médica) para su representación gráfica."
    )
    @ApiErroresLectura
    public ResponseEntity<List<DatoGraficoDTO>> obtenerAusenciasAprobadasPorTipo() {
        return ResponseEntity.ok(estadisticasService.obtenerAusenciasAprobadasPorTipo());
    }

    /**
     * Obtiene estadísticas sobre el estado de las solicitudes de ausencia (pendientes, aprobadas, denegadas).
     * <p>
     * Permite visualizar la carga de trabajo administrativo en la revisión de solicitudes.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/ausencias-estado}
     * </p>
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando los estados y su conteo.
     */
    @GetMapping("/ausencias-estado")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    @Operation(
            summary = "Estado de solicitudes de ausencia",
            description = "Requiere Token de EMPRESA o SUPERVISOR. Muestra el volumen de solicitudes de ausencia pendientes frente " +
                    "a aprobadas/denegadas para medir la carga administrativa."
    )
    @ApiErroresLectura
    public ResponseEntity<List<DatoGraficoDTO>> obtenerAusenciasPorEstado() {
        return ResponseEntity.ok(estadisticasService.obtenerAusenciasPorEstado());
    }

    /**
     * Obtiene un listado de los empleados con mayor número de retrasos acumulados en sus fichajes.
     * <p>
     * Información óptima para identificar desviaciones recurrentes en la puntualidad.
     * </p>
     * <p>
     * URL de acceso: {@code GET http://localhost:8080/api/estadisticas/top-retrasos}
     * </p>
     *
     * @return ResponseEntity con una lista de objetos {@link DatoGraficoDTO} representando el ranking de retrasos por empleado.
     */
    @GetMapping("/top-retrasos")
    @PreAuthorize("hasAnyRole('EMPRESA', 'SUPERVISOR')")
    @Operation(
            summary = "Ranking de retrasos",
            description = "Requiere Token de EMPRESA o SUPERVISOR. Identifica a los empleados con mayor número de " +
                    "impuntualidades acumuladas en sus fichajes."
    )
    @ApiErroresLectura
    public ResponseEntity<List<DatoGraficoDTO>> obtenerTopRetrasos() {
        return ResponseEntity.ok(estadisticasService.obtenerTopRetrasos());
    }
}
