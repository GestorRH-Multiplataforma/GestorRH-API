package com.gestorrh.api.service;

import com.gestorrh.api.dto.reporteDTO.ReporteDetalleDTO;
import com.gestorrh.api.dto.reporteDTO.ReporteResumenDTO;
import com.gestorrh.api.entity.Empleado;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.entity.Fichaje;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import com.gestorrh.api.repository.FichajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final FichajeRepository fichajeRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpleadoRepository empleadoRepository;

    private static final int MINUTOS_CORTESIA = 15;
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // OBTENER DETALLE (FILA A FILA)

    @Transactional(readOnly = true)
    public List<ReporteDetalleDTO> obtenerReporteDetallado(LocalDate fechaInicio, LocalDate fechaFin, Long idEmpleadoFiltro) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailAuth = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));
        boolean esSupervisor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"));

        List<Fichaje> fichajes;

        if (esEmpresa) {
            Empresa empresa = empresaRepository.findByEmail(emailAuth).orElseThrow();
            fichajes = fichajeRepository.findByEmpleadoEmpresaIdEmpresaAndFechaBetween(empresa.getIdEmpresa(), fechaInicio, fechaFin);

            if (idEmpleadoFiltro != null) {
                fichajes = fichajes.stream()
                        .filter(f -> f.getEmpleado().getIdEmpleado().equals(idEmpleadoFiltro)).collect(Collectors.toList());
            }
        } else {
            Empleado empleadoAuth = empleadoRepository.findByEmail(emailAuth).orElseThrow();

            if (esSupervisor) {
                List<Fichaje> fichajesDepto = fichajeRepository.findByEmpleadoEmpresaIdEmpresaAndFechaBetween(empleadoAuth.getEmpresa().getIdEmpresa(), fechaInicio, fechaFin);
                fichajes = fichajesDepto.stream()
                        .filter(f -> f.getEmpleado().getDepartamento().equalsIgnoreCase(empleadoAuth.getDepartamento()))
                        .collect(Collectors.toList());

                if (idEmpleadoFiltro != null) {
                    boolean permitido = fichajes.stream().anyMatch(f -> f.getEmpleado().getIdEmpleado().equals(idEmpleadoFiltro)) || empleadoAuth.getIdEmpleado().equals(idEmpleadoFiltro);
                    if (!permitido) throw new RuntimeException("Acceso denegado: Solo puedes ver a los empleados de tu departamento.");
                    fichajes = fichajes.stream().filter(f -> f.getEmpleado().getIdEmpleado().equals(idEmpleadoFiltro)).collect(Collectors.toList());
                }
            } else {
                fichajes = fichajeRepository.findByEmpleadoIdEmpleadoAndFechaBetween(empleadoAuth.getIdEmpleado(), fechaInicio, fechaFin);
            }
        }

        return fichajes.stream()
                .filter(f -> f.getHoraSalida() != null)
                .map(this::calcularFichaje)
                .collect(Collectors.toList());
    }

    // OBTENER RESUMEN (AGRUPADO PARA ESTADÍSTICAS Y PDF EJECUTIVO)

    @Transactional(readOnly = true)
    public List<ReporteResumenDTO> obtenerReporteResumen(LocalDate fechaInicio, LocalDate fechaFin, Long idEmpleadoFiltro) {

        List<ReporteDetalleDTO> detalle = obtenerReporteDetallado(fechaInicio, fechaFin, idEmpleadoFiltro);

        return detalle.stream()
                .collect(Collectors.groupingBy(ReporteDetalleDTO::getIdEmpleado))
                .values().stream()
                .map(listaFichajesEmpleado -> {
                    ReporteDetalleDTO primerFichaje = listaFichajesEmpleado.get(0);
                    return ReporteResumenDTO.builder()
                            .idEmpleado(primerFichaje.getIdEmpleado())
                            .nombreEmpleado(primerFichaje.getNombreEmpleado())
                            .departamento(primerFichaje.getDepartamento())
                            .diasTrabajados(listaFichajesEmpleado.size())
                            .totalTiempoTeoricoMinutos(listaFichajesEmpleado.stream().mapToLong(ReporteDetalleDTO::getTiempoTeoricoMinutos).sum())
                            .totalTiempoTotalMinutos(listaFichajesEmpleado.stream().mapToLong(ReporteDetalleDTO::getTiempoTotalMinutos).sum())
                            .totalMinutosExtra(listaFichajesEmpleado.stream().mapToLong(ReporteDetalleDTO::getMinutosExtra).sum())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // MÉTODOS PRIVADOS (CÁLCULO MATEMÁTICO)

    private ReporteDetalleDTO calcularFichaje(Fichaje f) {
        long minutosTotales = Duration.between(f.getHoraEntrada(), f.getHoraSalida()).toMinutes();
        long minutosTeoricos = 0;
        long minutosExtra = 0;
        String descripcionTurno = "Sin turno asignado (Fichaje Fantasma)";

        if (f.getAsignacion() != null) {
            descripcionTurno = f.getAsignacion().getTurno().getDescripcion();
            LocalTime inicioTurno = f.getAsignacion().getTurno().getHoraInicio();
            LocalTime finTurno = f.getAsignacion().getTurno().getHoraFin();

            minutosTeoricos = Duration.between(inicioTurno, finTurno).toMinutes();
            if (minutosTeoricos < 0) {
                minutosTeoricos += 24 * 60;
            }

            long minutosTotalesEfectivos = minutosTotales;
            LocalDateTime fechaHoraInicioTurno = f.getFecha().atTime(inicioTurno);

            if (f.getHoraEntrada().isBefore(fechaHoraInicioTurno)) {
                long minutosTemprano = Duration.between(f.getHoraEntrada(), fechaHoraInicioTurno).toMinutes();
                minutosTotalesEfectivos -= minutosTemprano;
            }

            long diferencia = minutosTotalesEfectivos - minutosTeoricos;

            if (diferencia > MINUTOS_CORTESIA) {
                minutosExtra = diferencia;
            }
        } else {
            minutosExtra = minutosTotales;
        }

        return ReporteDetalleDTO.builder()
                .idEmpleado(f.getEmpleado().getIdEmpleado())
                .nombreEmpleado(f.getEmpleado().getNombre() + " " + f.getEmpleado().getApellidos())
                .departamento(f.getEmpleado().getDepartamento() != null ? f.getEmpleado().getDepartamento() : "Sin departamento")
                .fecha(f.getFecha())
                .descripcionTurno(descripcionTurno)
                .horaEntradaReal(f.getHoraEntrada().format(HORA_FORMATTER))
                .horaSalidaReal(f.getHoraSalida().format(HORA_FORMATTER))
                .tiempoTotalMinutos(minutosTotales)
                .tiempoTeoricoMinutos(minutosTeoricos)
                .minutosExtra(minutosExtra)
                .incidencias(f.getIncidencias() != null ? f.getIncidencias() : "")
                .build();
    }
}
