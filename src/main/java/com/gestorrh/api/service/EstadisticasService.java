package com.gestorrh.api.service;

import com.gestorrh.api.dto.estadisticasDTO.DatoGraficoDTO;
import com.gestorrh.api.repository.*;
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

@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final AusenciaRepository ausenciaRepository;
    private final FichajeRepository fichajeRepository;
    private final AsignacionTurnoRepository asignacionTurnoRepository;

    // TARJETAS KPI

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

    // DATOS PARA GRÁFICOS

    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerEmpleadosPorDepartamento() {
        return empleadoRepository.contarEmpleadosPorDepartamento(obtenerIdEmpresaAutenticada());
    }

    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerAusenciasPorTipo() {
        List<Object[]> resultados = ausenciaRepository.contarAusenciasAprobadasPorTipo(obtenerIdEmpresaAutenticada());
        return mapearResultados(resultados);
    }

    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerAusenciasPorEstado() {
        List<Object[]> resultados = ausenciaRepository.contarAusenciasPorEstado(obtenerIdEmpresaAutenticada());
        return mapearResultados(resultados);
    }

    @Transactional(readOnly = true)
    public List<DatoGraficoDTO> obtenerTopRetrasos() {
        List<Object[]> resultados = fichajeRepository.obtenerTopRetrasos(obtenerIdEmpresaAutenticada());
        return resultados.stream()
                .limit(5)
                .map(obj -> new DatoGraficoDTO(obj[0].toString(), (Number) obj[1]))
                .collect(Collectors.toList());
    }

    // MÉTODOS PRIVADOS

    private List<DatoGraficoDTO> mapearResultados(List<Object[]> resultados) {
        return resultados.stream()
                .map(obj -> new DatoGraficoDTO(obj[0].toString(), (Number) obj[1]))
                .collect(Collectors.toList());
    }

    private Long obtenerIdEmpresaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        boolean esEmpresa = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPRESA"));

        if (esEmpresa) {
            return empresaRepository.findByEmail(email).orElseThrow().getIdEmpresa();
        } else {
            return empleadoRepository.findByEmail(email).orElseThrow().getEmpresa().getIdEmpresa();
        }
    }
}
