package com.gestorrh.api.service;

import com.gestorrh.api.dto.fichajeDTO.PeticionFichajeEntradaDTO;
import com.gestorrh.api.dto.fichajeDTO.PeticionFichajeSalidaDTO;
import com.gestorrh.api.dto.fichajeDTO.RespuestaFichajeDTO;
import com.gestorrh.api.entity.AsignacionTurno;
import com.gestorrh.api.entity.Empleado;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.entity.Fichaje;
import com.gestorrh.api.entity.enums.ModalidadTurno;
import com.gestorrh.api.repository.AsignacionTurnoRepository;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import com.gestorrh.api.repository.FichajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FichajeService {

    private final FichajeRepository fichajeRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final AsignacionTurnoRepository asignacionRepository;
    private final GeofencingService geofencingService;

    private static final int MINUTOS_CORTESIA = 15;

    // 1. LÓGICA DE ENTRADA Y SALIDA (SOLO MÓVIL: EMPLEADOS Y SUPERVISORES)

    @Transactional
    public RespuestaFichajeDTO ficharEntrada(PeticionFichajeEntradaDTO peticion) {
        Empleado empleado = obtenerEmpleadoAutenticado();
        validarSedeConfigurada(empleado.getEmpresa());
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();

        List<Fichaje> abiertos = fichajeRepository.findByEmpleadoIdEmpleadoAndFechaAndHoraSalidaIsNull(empleado.getIdEmpleado(), hoy);
        if (!abiertos.isEmpty()) {
            throw new RuntimeException("No puedes fichar la entrada: ya tienes un turno abierto sin registrar la salida.");
        }

        List<AsignacionTurno> asignacionesHoy = asignacionRepository.findByEmpleadoIdEmpleadoAndFecha(empleado.getIdEmpleado(), hoy);
        AsignacionTurno asignacionActual = asignacionesHoy.isEmpty() ? null : asignacionesHoy.get(0);

        String incidencias = "";

        if (asignacionActual != null) {
            if (asignacionActual.getModalidad() == ModalidadTurno.PRESENCIAL) {
                Empresa empresa = empleado.getEmpresa();
                boolean gpsValido = geofencingService.esFichajeValido(
                        peticion.getLatitud(), peticion.getLongitud(),
                        empresa.getLatitudSede(), empresa.getLongitudSede(), empresa.getRadioValidez()
                );

                if (!gpsValido) {
                    throw new RuntimeException("Fichaje denegado: Estás fuera del radio permitido de la oficina para tu turno presencial.");
                }
            }

            LocalDateTime horaInicioTurno = hoy.atTime(asignacionActual.getTurno().getHoraInicio());
            if (ahora.isAfter(horaInicioTurno.plusMinutes(MINUTOS_CORTESIA))) {
                incidencias = "Retraso en la entrada. Fichó a las " + ahora.toLocalTime() + ". ";
            }

        } else {
            incidencias = "Fichaje registrado sin turno asignado para el día de hoy. ";
        }

        Fichaje nuevoFichaje = Fichaje.builder()
                .empleado(empleado)
                .asignacion(asignacionActual)
                .fecha(hoy)
                .horaEntrada(ahora)
                .latitudEntrada(peticion.getLatitud())
                .longitudEntrada(peticion.getLongitud())
                .incidencias(incidencias.isEmpty() ? null : incidencias.trim())
                .build();

        nuevoFichaje = fichajeRepository.save(nuevoFichaje);
        return mapearARespuesta(nuevoFichaje);
    }

    @Transactional
    public RespuestaFichajeDTO ficharSalida(PeticionFichajeSalidaDTO peticion) {
        Empleado empleado = obtenerEmpleadoAutenticado();
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();

        List<Fichaje> abiertos = fichajeRepository.findByEmpleadoIdEmpleadoAndFechaAndHoraSalidaIsNull(empleado.getIdEmpleado(), hoy);
        if (abiertos.isEmpty()) {
            throw new RuntimeException("No puedes fichar la salida: no tienes ninguna entrada abierta para el día de hoy.");
        }

        Fichaje fichajeAbierto = abiertos.get(0);

        fichajeAbierto.setHoraSalida(ahora);
        fichajeAbierto.setLatitudSalida(peticion.getLatitud());
        fichajeAbierto.setLongitudSalida(peticion.getLongitud());

        if (fichajeAbierto.getAsignacion() != null) {
            LocalDateTime horaFinTurno = hoy.atTime(fichajeAbierto.getAsignacion().getTurno().getHoraFin());
            if (ahora.isBefore(horaFinTurno)) {
                String nuevaIncidencia = "Salida anticipada a las " + ahora.toLocalTime() + ". ";
                String incidenciasPrevias = fichajeAbierto.getIncidencias() == null ? "" : fichajeAbierto.getIncidencias() + " | ";
                fichajeAbierto.setIncidencias(incidenciasPrevias + nuevaIncidencia);
            }
        }

        fichajeAbierto = fichajeRepository.save(fichajeAbierto);
        return mapearARespuesta(fichajeAbierto);
    }

    // 2. LÓGICA DE CONSULTAS (ESCRITORIO / MÓVIL SEGÚN EL ROL)

    @Transactional(readOnly = true)
    public List<RespuestaFichajeDTO> consultarFichajes(LocalDate fechaInicio, LocalDate fechaFin, Long empleadoIdFiltro) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailAuth = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));
        boolean esSupervisor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"));

        List<Fichaje> fichajes;

        if (esEmpresa) {
            Empresa empresa = empresaRepository.findByEmail(emailAuth).orElseThrow();
            fichajes = fichajeRepository.findByEmpleadoEmpresaIdEmpresaAndFechaBetween(empresa.getIdEmpresa(), fechaInicio, fechaFin);

            if (empleadoIdFiltro != null) {
                fichajes = fichajes.stream().filter(f -> f.getEmpleado().getIdEmpleado().equals(empleadoIdFiltro)).collect(Collectors.toList());
            }

        } else {
            Empleado empleadoAuth = empleadoRepository.findByEmail(emailAuth).orElseThrow();

            if (esSupervisor) {
                List<Fichaje> fichajesDepartamento = fichajeRepository.findByEmpleadoEmpresaIdEmpresaAndFechaBetween(empleadoAuth.getEmpresa().getIdEmpresa(), fechaInicio, fechaFin);
                fichajes = fichajesDepartamento.stream()
                        .filter(f -> f.getEmpleado().getDepartamento().equalsIgnoreCase(empleadoAuth.getDepartamento()))
                        .collect(Collectors.toList());

                if (empleadoIdFiltro != null) {
                    boolean accesoPermitido = fichajes.stream().anyMatch(f -> f.getEmpleado().getIdEmpleado().equals(empleadoIdFiltro))
                            || empleadoAuth.getIdEmpleado().equals(empleadoIdFiltro);
                    if (!accesoPermitido) {
                        throw new RuntimeException("Acceso denegado: Solo puedes ver los fichajes de los empleados de tu departamento (" + empleadoAuth.getDepartamento() + ").");
                    }
                    fichajes = fichajes.stream().filter(f -> f.getEmpleado().getIdEmpleado().equals(empleadoIdFiltro)).collect(Collectors.toList());
                }

            } else {
                fichajes = fichajeRepository.findByEmpleadoIdEmpleadoAndFechaBetween(empleadoAuth.getIdEmpleado(), fechaInicio, fechaFin);
            }
        }

        return fichajes.stream().map(this::mapearARespuesta).collect(Collectors.toList());
    }

    // MÉTODOS PRIVADOS

    private Empleado obtenerEmpleadoAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return empleadoRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
    }

    private void validarSedeConfigurada(Empresa empresa) {
        if (empresa.getLatitudSede() == null || empresa.getLongitudSede() == null || empresa.getRadioValidez() == null) {
            throw new RuntimeException("Operación denegada: La empresa debe configurar la ubicación de su sede en el perfil antes de permitir registrar fichajes.");
        }
    }

    private RespuestaFichajeDTO mapearARespuesta(Fichaje f) {
        return RespuestaFichajeDTO.builder()
                .idFichaje(f.getIdFichaje())
                .idEmpleado(f.getEmpleado().getIdEmpleado())
                .nombreEmpleado(f.getEmpleado().getNombre() + " " + f.getEmpleado().getApellidos())
                .idAsignacion(f.getAsignacion() != null ? f.getAsignacion().getIdAsignacion() : null)
                .descripcionTurno(f.getAsignacion() != null ? f.getAsignacion().getTurno().getDescripcion() : "Sin turno asignado")
                .fecha(f.getFecha())
                .horaEntrada(f.getHoraEntrada())
                .horaSalida(f.getHoraSalida())
                .incidencias(f.getIncidencias())
                .build();
    }
}
