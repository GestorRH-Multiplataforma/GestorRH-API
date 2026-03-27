package com.gestorrh.api.service;

import com.gestorrh.api.dto.fichaje.PeticionFichajeEntradaDTO;
import com.gestorrh.api.dto.fichaje.PeticionFichajeSalidaDTO;
import com.gestorrh.api.dto.fichaje.PeticionModificacionFichajeDTO;
import com.gestorrh.api.dto.fichaje.RespuestaFichajeDTO;
import com.gestorrh.api.entity.AsignacionTurno;
import com.gestorrh.api.entity.Empleado;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.entity.Fichaje;
import com.gestorrh.api.entity.enums.ModalidadTurno;
import com.gestorrh.api.repository.AsignacionTurnoRepository;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import com.gestorrh.api.repository.FichajeRepository;
import jakarta.persistence.EntityNotFoundException;
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

/**
 * Servicio central para la gestión de fichajes y registros de jornada de los empleados.
 * <p>
 * Gestiona el ciclo completo de la jornada laboral: desde el registro de entrada con validación 
 * de geolocalización (geovallado) y detección de retrasos, hasta el registro de salida 
 * con cálculo automático de incidencias horarias.
 * </p>
 * <p>
 * Proporciona además funcionalidades críticas de auditoría para la modificación manual de fichajes
 * por parte de gestores, garantizando la trazabilidad de cualquier alteración en el registro de tiempos.
 * </p>
 */
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

    /**
     * Registra un nuevo inicio de jornada (fichaje de entrada) para el empleado autenticado.
     * <p>
     * El flujo de ejecución realiza las siguientes validaciones críticas:
     * </p>
     * <ol>
     *   <li><b>Configuración de Sede:</b> Verifica que la empresa haya configurado sus coordenadas GPS y radio de validez.</li>
     *   <li><b>Fichaje Duplicado:</b> Impide abrir una nueva jornada si ya existe un registro abierto (sin hora de salida) para hoy.</li>
     *   <li><b>Geovallado:</b> Si el turno asignado es de modalidad presencial, valida que la ubicación GPS recibida esté dentro del radio permitido respecto a la sede.</li>
     *   <li><b>Control de Puntualidad:</b> Compara la hora actual con el inicio del turno asignado (incluyendo un margen de cortesía de 15 minutos) para detectar posibles retrasos.</li>
     * </ol>
     *
     * @param peticion Objeto {@link PeticionFichajeEntradaDTO} con las coordenadas GPS (latitud y longitud) del empleado.
     * @return {@link RespuestaFichajeDTO} con los detalles del registro creado satisfactoriamente.
     * @throws RuntimeException Si se detecta una violación de geolocalización o si el estado del empleado/empresa es inválido para operar.
     */
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

    /**
     * Finaliza la jornada laboral actual registrando el fichaje de salida.
     * Calcula posibles incidencias por salida anticipada si el empleado cierra el turno antes de la hora prevista.
     *
     * @param peticion DTO con las coordenadas GPS del momento de salida.
     * @return RespuestaFichajeDTO con la información actualizada incluyendo la hora de salida.
     * @throws RuntimeException Si no hay ninguna entrada abierta para el día de hoy.
     */
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

    /**
     * Consulta el historial de fichajes bajo un rango de fechas y filtros opcionales.
     * La visibilidad de los datos depende del rol:
     * - EMPRESA: Ve todos los fichajes de su plantilla.
     * - SUPERVISOR: Ve fichajes de su departamento.
     * - EMPLEADO: Ve únicamente sus propios registros.
     *
     * @param fechaInicio Fecha de inicio de la búsqueda.
     * @param fechaFin Fecha de fin de la búsqueda.
     * @param empleadoIdFiltro Identificador opcional para filtrar por un empleado concreto.
     * @return List de RespuestaFichajeDTO con los registros localizados.
     */
    @Transactional(readOnly = true)
    public List<RespuestaFichajeDTO> consultarFichajes(LocalDate fechaInicio, LocalDate fechaFin, Long empleadoIdFiltro) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailAuth = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));
        boolean esSupervisor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"));

        List<Fichaje> fichajes;

        if (esEmpresa) {
            Empresa empresa = empresaRepository.findByEmail(emailAuth)
                    .orElseThrow(() -> new EntityNotFoundException("Error crítico: Empresa no encontrada en el sistema"));
            fichajes = fichajeRepository.findByEmpleadoEmpresaIdEmpresaAndFechaBetween(empresa.getIdEmpresa(), fechaInicio, fechaFin);

            if (empleadoIdFiltro != null) {
                fichajes = fichajes.stream().filter(f -> f.getEmpleado().getIdEmpleado().equals(empleadoIdFiltro)).collect(Collectors.toList());
            }

        } else {
            Empleado empleadoAuth = empleadoRepository.findByEmail(emailAuth)
                    .orElseThrow(() -> new EntityNotFoundException("Error crítico: Empleado no encontrado en el sistema"));

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

    /**
     * Permite a una empresa o supervisor modificar manualmente las horas de un fichaje ya registrado.
     * Esta operación está estrictamente auditada y requiere un motivo justificado. Los supervisores
     * no pueden modificar sus propios fichajes.
     *
     * @param idFichaje Identificador del registro a modificar.
     * @param peticion DTO con las nuevas horas y el motivo de la modificación.
     * @return RespuestaFichajeDTO con el registro actualizado y la marca de auditoría en incidencias.
     * @throws RuntimeException Si la modificación viola reglas de seguridad o jurisdicción de roles.
     */
    @Transactional
    public RespuestaFichajeDTO modificarFichajeManual(Long idFichaje, PeticionModificacionFichajeDTO peticion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailAuth = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));

        Fichaje fichaje = fichajeRepository.findById(idFichaje)
                .orElseThrow(() -> new EntityNotFoundException("Fichaje no encontrado"));

        if (esEmpresa) {
            Empresa empresa = empresaRepository.findByEmail(emailAuth)
                    .orElseThrow(() -> new EntityNotFoundException("Error crítico: Empresa no encontrada en el sistema"));
            if (!fichaje.getEmpleado().getEmpresa().getIdEmpresa().equals(empresa.getIdEmpresa())) {
                log.warn("VIOLACIÓN DE SEGURIDAD: La empresa '{}' intentó modificar el fichaje ID {} de otra empresa.", emailAuth, idFichaje);
                throw new RuntimeException("Acceso denegado: El fichaje pertenece a otra empresa.");
            }
        } else {
            Empleado supervisor = empleadoRepository.findByEmail(emailAuth)
                    .orElseThrow(() -> new EntityNotFoundException("Error crítico: Supervisor no encontrado en el sistema"));

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

    /**
     * Obtiene la entidad {@link Empleado} asociada al usuario autenticado en el sistema.
     *
     * @return Empleado que realiza la operación.
     */
    private Empleado obtenerEmpleadoAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return empleadoRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado"));
    }

    /**
     * Valida que la empresa tenga configurada la ubicación de su sede.
     * Es un requisito indispensable para poder realizar fichajes con geovallado.
     *
     * @param empresa La entidad empresa a validar.
     */
    private void validarSedeConfigurada(Empresa empresa) {
        if (empresa.getLatitudSede() == null || empresa.getLongitudSede() == null || empresa.getRadioValidez() == null) {
            throw new RuntimeException("Operación denegada: La empresa debe configurar la ubicación de su sede en el perfil antes de permitir registrar fichajes.");
        }
    }

    /**
     * Mapea una entidad {@link Fichaje} a su correspondiente DTO de respuesta.
     *
     * @param f Entidad de base de datos.
     * @return RespuestaFichajeDTO formateado para la API.
     */
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
