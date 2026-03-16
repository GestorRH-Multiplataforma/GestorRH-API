package com.gestorrh.api.dto.fichajeDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaFichajeDTO {

    private Long idFichaje;
    private Long idEmpleado;
    private String nombreEmpleado;

    private Long idAsignacion;
    private String descripcionTurno;

    private LocalDate fecha;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSalida;

    private String incidencias;
}
