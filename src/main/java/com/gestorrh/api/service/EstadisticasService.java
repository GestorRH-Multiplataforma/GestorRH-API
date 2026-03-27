package com.gestorrh.api.service;

import com.gestorrh.api.dto.estadisticas.DatoGraficoDTO;
import com.gestorrh.api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la generación de métricas y datos estadísticos estructurados para el panel de control.
 * <p>
 * Proporciona Indicadores Clave de Rendimiento (KPIs) en tiempo real, así como desgloses 
 * agregados para su representación gráfica (gráficos de tarta, de barras, etc.). 
 * </p>
 * <p>
 * Permite a las empresas visualizar de un vistazo el estado de su plantilla, incluyendo la puntualidad, 
 * el ausentismo y la distribución departamental, garantizando un soporte sólido para la toma de decisiones gerenciales.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final AusenciaRepository ausenciaRepository;
    private final FichajeRepository fichajeRepository;
    private final AsignacionTurnoRepository asignacionTurnoRepository;

    /**
     * Recupera los indicadores clave de rendimiento (KPIs) globales consolidados para la fecha actual.
     * <p>
     * Las métricas proporcionadas incluyen:
     * </p>
     * <ul>
     *   <li>{@code totalEmpleados}: Cantidad de trabajadores activos en la empresa.</li>
     *   <li>{@code planificadosHoy}: Número de trabajadores que tienen al menos un turno asignado para hoy.</li>
     *   <li>{@code ausentesHoy}: Conteo de trabajadores con ausencias aprobadas vigentes en la fecha actual.</li>
     * </ul>
     *
     * @return Map con las etiquetas descriptivas de los KPIs como clave y sus correspondientes valores numéricos.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> obtenerKpisDashboard() {
        Long idEmpresa = obtenerIdEmpresaAutenticada();
        LocalDate hoy = LocalDate.now();

        Map<String, Long> kpis = new HashMap<>();
        kpis.put("totalEmpleados", empleadoRepository.contarTotalEmpleadosActivos(idEmpresa));
        kpis.put("planificadosHoy", asignacionTurnoRepository.contarEmpleadosPlanificadosHoy(idEmpresa, hoy));
        kpis.put("ausentesHoy", ausenciaRepository.contarEmpleadosAusentesHoy(idEmpresa, hoy));

        return kpis;
    }

    /**
     * Obtiene el desglose de empleados por departamento para su representación en gráficos (ej. Pie Chart).
     *
     * @return List de DatoGraficoDTO con el nombre del departamento y el número de empleados asociados.
     */
    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerEmpleadosPorDepartamento() {
        return empleadoRepository.contarEmpleadosPorDepartamento(obtenerIdEmpresaAutenticada());
    }

    /**
     * Recupera el conteo de ausencias aprobadas agrupadas por su tipo (Vacaciones, Enfermedad, etc.).
     *
     * @return List de DatoGraficoDTO con la distribución por tipo de ausencia.
     */
    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerAusenciasAprobadasPorTipo() {
        List<Object[]> resultados = ausenciaRepository.contarAusenciasAprobadasPorTipo(obtenerIdEmpresaAutenticada());
        return mapearResultados(resultados);
    }

    /**
     * Obtiene la distribución de ausencias según su estado actual (Solicitada, Aprobada, Rechazada).
     *
     * @return List de DatoGraficoDTO con los totales por estado.
     */
    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerAusenciasPorEstado() {
        List<Object[]> resultados = ausenciaRepository.contarAusenciasPorEstado(obtenerIdEmpresaAutenticada());
        return mapearResultados(resultados);
    }

    /**
     * Identifica a los empleados con mayor número de retrasos acumulados en sus fichajes de entrada.
     * Los resultados se limitan a los 5 casos más destacados.
     *
     * @return List de DatoGraficoDTO con el nombre del empleado y el conteo de sus retrasos.
     */
    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerTopRetrasos() {
        List<Object[]> resultados = fichajeRepository.obtenerTopRetrasos(obtenerIdEmpresaAutenticada());
        return resultados.stream()
                .limit(5)
                .map(obj -> new DatoGraficoDTO(obj[0].toString(), (Number) obj[1]))
                .collect(Collectors.toList());
    }

    /**
     * Utilidad privada para transformar resultados brutos de consultas JPA (Object[]) en DTOs para gráficos.
     *
     * @param resultados Lista de arreglos de objetos devueltos por la base de datos.
     * @return List de DatoGraficoDTO mapeada.
     */
    private List<DatoGraficoDTO> mapearResultados(List<Object[]> resultados) {
        return resultados.stream()
                .map(obj -> new DatoGraficoDTO(obj[0].toString(), (Number) obj[1]))
                .collect(Collectors.toList());
    }

    /**
     * Recupera el identificador de la empresa del usuario autenticado.
     * Si el usuario es un empleado o supervisor, se obtiene el ID de la empresa a la que pertenece.
     *
     * @return Long El identificador único de la empresa asociada al contexto de seguridad.
     * @throws EntityNotFoundException Si la empresa o el empleado no existen en el sistema.
     */
    private Long obtenerIdEmpresaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));

        if (esEmpresa) {
            return empresaRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Error crítico: Empresa no encontrada en el sistema")).getIdEmpresa();
        } else {
            return empleadoRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Error crítico: Empleado no encontrado en el sistema")).getEmpresa().getIdEmpresa();
        }
    }
}
