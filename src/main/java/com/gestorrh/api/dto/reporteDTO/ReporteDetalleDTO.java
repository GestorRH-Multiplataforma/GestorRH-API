package com.gestorrh.api.dto.reporteDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO Plano (sin anidaciones) diseñado específicamente para ser
 * inyectado en JasperReports o tablas de interfaces gráficas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteDetalleDTO {

    private Long idEmpleado;
    private String nombreEmpleado;
    private String departamento;
    private LocalDate fecha;
    private String descripcionTurno;
    private String horaEntradaReal;
    private String horaSalidaReal;

    private Long tiempoTotalMinutos;
    private Long tiempoTeoricoMinutos;
    private Long minutosExtra;

    private String incidencias;
}
