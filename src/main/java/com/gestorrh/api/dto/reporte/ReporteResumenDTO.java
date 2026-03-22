package com.gestorrh.api.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteResumenDTO {

    private Long idEmpleado;
    private String nombreEmpleado;
    private String departamento;

    private int diasTrabajados;
    private Long totalTiempoTeoricoMinutos;
    private Long totalTiempoTotalMinutos;
    private Long totalMinutosExtra;
}
