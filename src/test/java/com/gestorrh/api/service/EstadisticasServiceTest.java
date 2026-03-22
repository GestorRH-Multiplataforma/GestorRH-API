package com.gestorrh.api.service;

import com.gestorrh.api.dto.estadisticas.DatoGraficoDTO;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private AusenciaRepository ausenciaRepository;
    @Mock
    private FichajeRepository fichajeRepository;
    @Mock
    private AsignacionTurnoRepository asignacionTurnoRepository;

    @InjectMocks
    private EstadisticasService estadisticasService;

    private Empresa empresaMock;

    @BeforeEach
    void setUp() {

        empresaMock = new Empresa();
        empresaMock.setIdEmpresa(1L);
        empresaMock.setNombre("Tech Corp");

        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPRESA"));
        when(auth.getAuthorities()).thenAnswer(invocation -> authorities);
        when(auth.getName()).thenReturn("admin@techcorp.com");

        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(empresaRepository.findByEmail("admin@techcorp.com")).thenReturn(Optional.of(empresaMock));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testObtenerKpisDashboard() {
        when(empleadoRepository.contarTotalEmpleadosActivos(1L)).thenReturn(45L);
        when(asignacionTurnoRepository.contarEmpleadosPlanificadosHoy(eq(1L), any(LocalDate.class))).thenReturn(40L);
        when(ausenciaRepository.contarEmpleadosAusentesHoy(eq(1L), any(LocalDate.class))).thenReturn(5L);

        Map<String, Long> kpis = estadisticasService.obtenerKpisDashboard();

        assertNotNull(kpis);
        assertEquals(45L, kpis.get("totalEmpleados"));
        assertEquals(40L, kpis.get("planificadosHoy"));
        assertEquals(5L, kpis.get("ausentesHoy"));
    }

    @Test
    void testObtenerEmpleadosPorDepartamento() {
        List<DatoGraficoDTO> mockDatos = Arrays.asList(
                new DatoGraficoDTO("IT", 15L),
                new DatoGraficoDTO("RRHH", 5L)
        );
        when(empleadoRepository.contarEmpleadosPorDepartamento(1L)).thenReturn(mockDatos);

        List<DatoGraficoDTO> resultado = estadisticasService.obtenerEmpleadosPorDepartamento();

        assertEquals(2, resultado.size());
        assertEquals("IT", resultado.get(0).getEtiqueta());
        assertEquals(15L, resultado.get(0).getValor());
    }

    @Test
    void testObtenerTopRetrasosLimitadoA5() {
        List<Object[]> mockResultados = Arrays.asList(
                new Object[]{"Juan", 10L},
                new Object[]{"Ana", 8L},
                new Object[]{"Pedro", 6L},
                new Object[]{"Maria", 5L},
                new Object[]{"Luis", 4L},
                new Object[]{"Carlos", 2L},
                new Object[]{"Lucia", 1L}
        );
        when(fichajeRepository.obtenerTopRetrasos(1L)).thenReturn(mockResultados);

        List<DatoGraficoDTO> resultado = estadisticasService.obtenerTopRetrasos();

        assertEquals(5, resultado.size(), "Debe limitar el resultado estrictamente a los 5 primeros");
        assertEquals("Juan", resultado.get(0).getEtiqueta());
        assertEquals(10L, resultado.get(0).getValor());
        assertEquals("Luis", resultado.get(4).getEtiqueta()); // El 5º elemento
    }
}
