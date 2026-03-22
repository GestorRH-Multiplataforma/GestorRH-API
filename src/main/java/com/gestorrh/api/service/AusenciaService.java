package com.gestorrh.api.service;

import com.gestorrh.api.dto.ausencia.PeticionAusenciaDTO;
import com.gestorrh.api.dto.ausencia.PeticionRevisionAusenciaDTO;
import com.gestorrh.api.dto.ausencia.RespuestaAusenciaDTO;
import com.gestorrh.api.entity.*;
import com.gestorrh.api.entity.enums.EstadoAusencia;
import com.gestorrh.api.repository.AsignacionTurnoRepository;
import com.gestorrh.api.repository.AusenciaRepository;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal para la Épica E6: Gestión de Ausencias.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AusenciaService {

    private final AusenciaRepository ausenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final AsignacionTurnoRepository asignacionRepository;
    private final FileStorageService fileStorageService;

    // MÉTODOS DEL EMPLEADO

    @Transactional
    public RespuestaAusenciaDTO crearAusencia(PeticionAusenciaDTO peticion, org.springframework.web.multipart.MultipartFile archivo) {
        Empleado empleadoLogueado = obtenerEmpleadoAutenticado();
        validarFechas(peticion.getFechaInicio(), peticion.getFechaFin());
        validarSolapamientoAusencias(empleadoLogueado.getIdEmpleado(), peticion.getFechaInicio(), peticion.getFechaFin(), null);

        String nombreArchivoGenerado = null;
        if (archivo != null && !archivo.isEmpty()) {
            nombreArchivoGenerado = fileStorageService.guardarArchivo(archivo);
        }

        Ausencia nuevaAusencia = Ausencia.builder()
                .empleado(empleadoLogueado)
                .tipo(peticion.getTipo())
                .descripcion(peticion.getDescripcion())
                .fechaInicio(peticion.getFechaInicio())
                .fechaFin(peticion.getFechaFin())
                .justificante(nombreArchivoGenerado)
                .estado(EstadoAusencia.SOLICITADA)
                .build();

        nuevaAusencia = ausenciaRepository.save(nuevaAusencia);

        log.info("NUEVA AUSENCIA: El empleado '{}' ha solicitado una ausencia de tipo [{}] del {} al {}.",
                empleadoLogueado.getEmail(), peticion.getTipo(), peticion.getFechaInicio(), peticion.getFechaFin());

        return mapearARespuesta(nuevaAusencia);
    }

    @Transactional(readOnly = true)
    public List<RespuestaAusenciaDTO> obtenerMisAusencias(EstadoAusencia estadoFiltro) {
        Empleado empleadoLogueado = obtenerEmpleadoAutenticado();
        List<Ausencia> misAusencias;

        if (estadoFiltro != null) {
            misAusencias = ausenciaRepository.findByEmpleadoIdEmpleadoAndEstado(empleadoLogueado.getIdEmpleado(), estadoFiltro);
        } else {
            misAusencias = ausenciaRepository.findByEmpleadoIdEmpleado(empleadoLogueado.getIdEmpleado());
        }

        return misAusencias.stream().map(this::mapearARespuesta).collect(Collectors.toList());
    }

    @Transactional
    public RespuestaAusenciaDTO actualizarMiAusencia(Long idAusencia, PeticionAusenciaDTO peticion, org.springframework.web.multipart.MultipartFile archivoNuevo) {
        Empleado empleadoLogueado = obtenerEmpleadoAutenticado();
        Ausencia ausencia = obtenerAusenciaPropia(idAusencia, empleadoLogueado);

        if (ausencia.getEstado() != EstadoAusencia.SOLICITADA) {
            log.warn("Operación DENEGADA: El empleado '{}' intentó modificar la ausencia ID {} que ya no está en estado SOLICITADA.", empleadoLogueado.getEmail(), idAusencia);
            throw new RuntimeException("Solo puedes modificar una ausencia que esté en estado SOLICITADA.");
        }

        validarFechas(peticion.getFechaInicio(), peticion.getFechaFin());
        validarSolapamientoAusencias(empleadoLogueado.getIdEmpleado(), peticion.getFechaInicio(), peticion.getFechaFin(), idAusencia);

        if (archivoNuevo != null && !archivoNuevo.isEmpty()) {
            if (ausencia.getJustificante() != null) {
                fileStorageService.eliminarArchivo(ausencia.getJustificante());
            }
            String nombreArchivoGenerado = fileStorageService.guardarArchivo(archivoNuevo);
            ausencia.setJustificante(nombreArchivoGenerado);
        }

        ausencia.setTipo(peticion.getTipo());
        ausencia.setDescripcion(peticion.getDescripcion());
        ausencia.setFechaInicio(peticion.getFechaInicio());
        ausencia.setFechaFin(peticion.getFechaFin());

        ausencia = ausenciaRepository.save(ausencia);

        log.info("AUSENCIA MODIFICADA: El empleado '{}' ha actualizado su solicitud de ausencia ID {}.", empleadoLogueado.getEmail(), idAusencia);

        return mapearARespuesta(ausencia);
    }

    @Transactional
    public void eliminarMiAusencia(Long idAusencia) {
        Empleado empleadoLogueado = obtenerEmpleadoAutenticado();
        Ausencia ausencia = obtenerAusenciaPropia(idAusencia, empleadoLogueado);

        if (ausencia.getEstado() != EstadoAusencia.SOLICITADA) {
            log.warn("Operación DENEGADA: El empleado '{}' intentó cancelar la ausencia ID {} que ya no está en estado SOLICITADA.", empleadoLogueado.getEmail(), idAusencia);
            throw new RuntimeException("Solo puedes cancelar una ausencia que esté en estado SOLICITADA.");
        }

        if (ausencia.getJustificante() != null) {
            fileStorageService.eliminarArchivo(ausencia.getJustificante());
        }

        ausenciaRepository.delete(ausencia);
        log.info("AUSENCIA CANCELADA: El empleado '{}' ha eliminado su solicitud de ausencia ID {}.", empleadoLogueado.getEmail(), idAusencia);
    }

    // MÉTODOS DE LA EMPRESA Y SUPERVISORES

    @Transactional(readOnly = true)
    public List<RespuestaAusenciaDTO> obtenerAusenciasPermitidas(EstadoAusencia estadoFiltro) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailAuth = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));

        List<Ausencia> ausencias;

        if (esEmpresa) {
            Empresa empresa = empresaRepository.findByEmail(emailAuth).orElseThrow();
            if (estadoFiltro != null) {
                ausencias = ausenciaRepository.findByEmpleadoEmpresaIdEmpresaAndEstado(empresa.getIdEmpresa(), estadoFiltro);
            } else {
                ausencias = ausenciaRepository.findByEmpleadoEmpresaIdEmpresa(empresa.getIdEmpresa());
            }
        } else {
            Empleado supervisor = empleadoRepository.findByEmail(emailAuth).orElseThrow();
            if (estadoFiltro != null) {
                ausencias = ausenciaRepository.findByEmpleadoEmpresaIdEmpresaAndEmpleadoDepartamentoIgnoreCaseAndEstado(
                        supervisor.getEmpresa().getIdEmpresa(), supervisor.getDepartamento(), estadoFiltro);
            } else {
                ausencias = ausenciaRepository.findByEmpleadoEmpresaIdEmpresaAndEmpleadoDepartamentoIgnoreCase(
                        supervisor.getEmpresa().getIdEmpresa(), supervisor.getDepartamento());
            }

            ausencias = ausencias.stream()
                    .filter(a -> !a.getEmpleado().getIdEmpleado().equals(supervisor.getIdEmpleado()))
                    .collect(Collectors.toList());
        }

        return ausencias.stream().map(this::mapearARespuesta).collect(Collectors.toList());
    }

    @Transactional
    public RespuestaAusenciaDTO revisarAusencia(Long idAusencia, PeticionRevisionAusenciaDTO peticion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailAuth = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));

        Ausencia ausencia = ausenciaRepository.findById(idAusencia)
                .orElseThrow(() -> new RuntimeException("Ausencia no encontrada"));

        validarPrivilegiosRevision(emailAuth, esEmpresa, ausencia.getEmpleado());

        if (peticion.getEstado() == EstadoAusencia.RECHAZADA &&
                (peticion.getObservacionesRevision() == null || peticion.getObservacionesRevision().trim().isEmpty())) {
            log.warn("Revisión DENEGADA: El usuario '{}' intentó rechazar la ausencia ID {} sin justificar los motivos.", emailAuth, idAusencia);
            throw new RuntimeException("Debes proporcionar observaciones si rechazas la ausencia.");
        }

        ausencia.setEstado(peticion.getEstado());
        ausencia.setResponsableRevision(emailAuth);
        ausencia.setObservacionesRevision(peticion.getObservacionesRevision());

        if (peticion.getEstado() == EstadoAusencia.APROBADA) {
            List<AsignacionTurno> turnosSolapados = asignacionRepository.findByEmpleadoIdEmpleadoAndFechaBetween(
                    ausencia.getEmpleado().getIdEmpleado(),
                    ausencia.getFechaInicio(),
                    ausencia.getFechaFin()
            );
            if (!turnosSolapados.isEmpty()) {
                asignacionRepository.deleteAll(turnosSolapados);
                log.info("SISTEMA: Eliminados {} turnos planificados para el empleado ID {} debido a la aprobación de la ausencia ID {}.",
                        turnosSolapados.size(), ausencia.getEmpleado().getIdEmpleado(), idAusencia);
            }
        }

        ausencia = ausenciaRepository.save(ausencia);

        log.info("REVISIÓN DE AUSENCIA: La ausencia ID {} ha sido marcada como {} por el usuario '{}'.",
                idAusencia, peticion.getEstado(), emailAuth);

        return mapearARespuesta(ausencia);
    }

    // MÉTODOS PRIVADOS

    private Empleado obtenerEmpleadoAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return empleadoRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
    }

    private Ausencia obtenerAusenciaPropia(Long idAusencia, Empleado empleadoLogueado) {
        Ausencia ausencia = ausenciaRepository.findById(idAusencia)
                .orElseThrow(() -> new RuntimeException("Ausencia no encontrada"));

        if (!ausencia.getEmpleado().getIdEmpleado().equals(empleadoLogueado.getIdEmpleado())) {
            throw new RuntimeException("Acceso denegado: Esta ausencia no te pertenece.");
        }
        return ausencia;
    }

    private void validarFechas(java.time.LocalDate inicio, java.time.LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new RuntimeException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
    }

    private void validarPrivilegiosRevision(String emailAuth, boolean esEmpresa, Empleado empleadoDestino) {
        if (esEmpresa) {
            Empresa empresa = empresaRepository.findByEmail(emailAuth).orElseThrow();
            if (!empleadoDestino.getEmpresa().getIdEmpresa().equals(empresa.getIdEmpresa())) {
                log.warn("VIOLACIÓN DE SEGURIDAD: La empresa '{}' intentó revisar una ausencia del empleado ID {}, que pertenece a otra empresa.", emailAuth, empleadoDestino.getIdEmpleado());
                throw new RuntimeException("El empleado no pertenece a tu empresa.");
            }
        } else {
            Empleado supervisor = empleadoRepository.findByEmail(emailAuth).orElseThrow();
            if (!supervisor.getEmpresa().getIdEmpresa().equals(empleadoDestino.getEmpresa().getIdEmpresa())) {
                throw new RuntimeException("El empleado no pertenece a tu empresa.");
            }
            if (!supervisor.getDepartamento().equalsIgnoreCase(empleadoDestino.getDepartamento())) {
                log.warn("VIOLACIÓN DE SEGURIDAD: El supervisor '{}' intentó revisar una ausencia de un empleado fuera de su departamento ({}).", emailAuth, empleadoDestino.getDepartamento());
                throw new RuntimeException("Solo puedes revisar ausencias de tu departamento.");
            }
            if (supervisor.getIdEmpleado().equals(empleadoDestino.getIdEmpleado())) {
                log.warn("DENEGADO: El supervisor '{}' intentó auto-aprobarse una ausencia.", emailAuth);
                throw new RuntimeException("No puedes aprobar o rechazar tus propias ausencias. Debe hacerlo la Empresa.");
            }
        }
    }

    private void validarSolapamientoAusencias(Long idEmpleado, java.time.LocalDate inicio, java.time.LocalDate fin, Long idAusenciaExcluida) {
        List<EstadoAusencia> estadosActivos = List.of(EstadoAusencia.SOLICITADA, EstadoAusencia.APROBADA);

        List<Ausencia> solapadas = ausenciaRepository.findAusenciasSolapadas(idEmpleado, estadosActivos, inicio, fin);

        if (idAusenciaExcluida != null) {
            solapadas = solapadas.stream()
                    .filter(a -> !a.getIdAusencia().equals(idAusenciaExcluida))
                    .collect(Collectors.toList());
        }

        if (!solapadas.isEmpty()) {
            throw new RuntimeException("No puedes solicitar esta ausencia: ya tienes otra solicitud (Pendiente o Aprobada) que coincide con estas fechas.");
        }
    }

    private RespuestaAusenciaDTO mapearARespuesta(Ausencia a) {
        return RespuestaAusenciaDTO.builder()
                .idAusencia(a.getIdAusencia())
                .idEmpleado(a.getEmpleado().getIdEmpleado())
                .nombreCompletoEmpleado(a.getEmpleado().getNombre() + " " + a.getEmpleado().getApellidos())
                .tipo(a.getTipo())
                .descripcion(a.getDescripcion())
                .fechaInicio(a.getFechaInicio())
                .fechaFin(a.getFechaFin())
                .justificante(a.getJustificante())
                .estado(a.getEstado())
                .responsableRevision(a.getResponsableRevision())
                .observacionesRevision(a.getObservacionesRevision())
                .build();
    }
}
