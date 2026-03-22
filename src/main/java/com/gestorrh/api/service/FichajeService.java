package com.gestorrh.api.service;

import com.gestorrh.api.dto.fichajeDTO.PeticionFichajeEntradaDTO;
import com.gestorrh.api.dto.fichajeDTO.PeticionFichajeSalidaDTO;
import com.gestorrh.api.dto.fichajeDTO.PeticionModificacionFichajeDTO;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            log.warn("Fichaje DENEGADO (Entrada): El empleado '{}' intentó abrir un turno, pero ya tiene uno abierto sin cerrar.", empleado.getEmail());
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
                    log.warn("Fichaje DENEGADO (Geovallado): El empleado '{}' intentó fichar PRESENCIAL fuera del radio permitido.", empleado.getEmail());
                    throw new RuntimeException("Fichaje denegado: Estás fuera del radio permitido de la oficina para tu turno presencial.");
                }
            }

            LocalDateTime horaInicioTurno = hoy.atTime(asignacionActual.getTurno().getHoraInicio());
            if (ahora.isAfter(horaInicioTurno.plusMinutes(MINUTOS_CORTESIA))) {
                incidencias = "Retraso en la entrada. Fichó a las " + ahora.toLocalTime() + ". ";
                log.info("Fichaje ENTRADA (CON RETRASO): El empleado '{}' ha fichado a las {}.", empleado.getEmail(), ahora.toLocalTime());
            } else {
                log.info("Fichaje ENTRADA: El empleado '{}' ha fichado correctamente.", empleado.getEmail());
            }

        } else {
            incidencias = "Fichaje registrado sin turno asignado para el día de hoy. ";
            log.info("Fichaje ENTRADA (SIN TURNO): El empleado '{}' ha fichado sin tener turno planificado.", empleado.getEmail());
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
            log.warn("Fichaje DENEGADO (Salida): El empleado '{}' intentó fichar la salida sin tener una entrada abierta.", empleado.getEmail());
            throw new RuntimeException("No puedes fichar la salida: no tienes ninguna entrada abierta para el día de hoy.");
        }

        Fichaje fichajeAbierto = abiertos.get(0);

        fichajeAbierto.setHoraSalida(ahora);
        fichajeAbierto.setLatitudSalida(peticion.getLatitud());
        fichajeAbierto.setLongitudSalida(peticion.getLongitud());

        if (fichajeAbierto.getAsignacion() != null) {
            LocalDateTime horaFinTurno = hoy.atTime(fichajeAbierto.getAsignacion().getTurno().getHoraFin());

            if (ahora.isBefore(horaFinTurno.minusMinutes(MINUTOS_CORTESIA))) {
                String nuevaIncidencia = "Salida anticipada a las " + ahora.toLocalTime() + ". ";
                String incidenciasPrevias = fichajeAbierto.getIncidencias() == null ? "" : fichajeAbierto.getIncidencias() + " | ";
                fichajeAbierto.setIncidencias(incidenciasPrevias + nuevaIncidencia);
                log.info("Fichaje SALIDA (ANTICIPADA): El empleado '{}' ha salido antes de tiempo a las {}.", empleado.getEmail(), ahora.toLocalTime());
            } else {
                log.info("Fichaje SALIDA: El empleado '{}' ha cerrado su turno correctamente.", empleado.getEmail());
            }
        } else {
            log.info("Fichaje SALIDA: El empleado '{}' ha cerrado su turno (no planificado).", empleado.getEmail());
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

    @Transactional
    public RespuestaFichajeDTO modificarFichajeManual(Long idFichaje, PeticionModificacionFichajeDTO peticion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailAuth = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));

        Fichaje fichaje = fichajeRepository.findById(idFichaje)
                .orElseThrow(() -> new RuntimeException("Fichaje no encontrado"));

        if (esEmpresa) {
            Empresa empresa = empresaRepository.findByEmail(emailAuth).orElseThrow();
            if (!fichaje.getEmpleado().getEmpresa().getIdEmpresa().equals(empresa.getIdEmpresa())) {
                log.warn("VIOLACIÓN DE SEGURIDAD: La empresa '{}' intentó modificar el fichaje ID {} de otra empresa.", emailAuth, idFichaje);
                throw new RuntimeException("Acceso denegado: El fichaje pertenece a otra empresa.");
            }
        } else {
            Empleado supervisor = empleadoRepository.findByEmail(emailAuth).orElseThrow();

            if (fichaje.getEmpleado().getIdEmpleado().equals(supervisor.getIdEmpleado())) {
                log.warn("DENEGADO: El supervisor '{}' intentó modificar su propio fichaje ID {}.", emailAuth, idFichaje);
                throw new RuntimeException("Conflicto de intereses: Como supervisor no puedes modificar tus propios fichajes. Debe hacerlo la empresa u otro supervisor.");
            }

            if (!fichaje.getEmpleado().getEmpresa().getIdEmpresa().equals(supervisor.getEmpresa().getIdEmpresa()) ||
                    !fichaje.getEmpleado().getDepartamento().equalsIgnoreCase(supervisor.getDepartamento())) {
                log.warn("VIOLACIÓN DE SEGURIDAD: El supervisor '{}' intentó modificar el fichaje ID {} fuera de su jurisdicción.", emailAuth, idFichaje);
                throw new RuntimeException("Acceso denegado: Solo puedes modificar fichajes de los empleados de tu departamento.");
            }
        }

        if (peticion.getNuevaHoraEntrada() == null && peticion.getNuevaHoraSalida() == null) {
            throw new RuntimeException("Debes proporcionar al menos una nueva hora de entrada o de salida para modificar el fichaje.");
        }

        if (peticion.getNuevaHoraEntrada() != null) {
            fichaje.setHoraEntrada(peticion.getNuevaHoraEntrada());
        }
        if (peticion.getNuevaHoraSalida() != null) {
            fichaje.setHoraSalida(peticion.getNuevaHoraSalida());
        }

        String marcaAuditoria = String.format(" | [MODIFICADO MANUALMENTE por %s. Motivo: %s]", emailAuth, peticion.getMotivoModificacion());
        String incidenciasActuales = fichaje.getIncidencias() == null ? "" : fichaje.getIncidencias();

        if (incidenciasActuales.isEmpty()) {
            fichaje.setIncidencias(marcaAuditoria.replace(" | ", "").trim());
        } else {
            fichaje.setIncidencias(incidenciasActuales + marcaAuditoria);
        }

        fichaje = fichajeRepository.save(fichaje);

        log.info("AUDITORÍA: El fichaje ID {} ha sido MODIFICADO MANUALMENTE por '{}'. Motivo: {}", idFichaje, emailAuth, peticion.getMotivoModificacion());

        return mapearARespuesta(fichaje);
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
