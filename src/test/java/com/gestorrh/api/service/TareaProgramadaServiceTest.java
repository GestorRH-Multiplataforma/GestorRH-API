package com.gestorrh.api.service;

import com.gestorrh.api.repository.EmpleadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TareaProgramadaServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @InjectMocks
    private TareaProgramadaService tareaProgramadaService;

    @Test
    void actualizarEstadoEmpleados_ConEmpleadosCaducados_EjecutaCorrectamente() {
        when(empleadoRepository.desactivarEmpleadosConContratoExpirado()).thenReturn(3);

        tareaProgramadaService.actualizarEstadoEmpleados();

        verify(empleadoRepository, times(1)).desactivarEmpleadosConContratoExpirado();
    }

    @Test
    void actualizarEstadoEmpleados_SinEmpleadosCaducados_NoFalla() {
        when(empleadoRepository.desactivarEmpleadosConContratoExpirado()).thenReturn(0);

        tareaProgramadaService.actualizarEstadoEmpleados();

        verify(empleadoRepository, times(1)).desactivarEmpleadosConContratoExpirado();
    }

    @Test
    void actualizarEstadoEmpleados_CuandoBDEstaCaida_LanzaExcepcion() {
        when(empleadoRepository.desactivarEmpleadosConContratoExpirado())
                .thenThrow(new RuntimeException("Timeout de Base de Datos"));

        assertThrows(RuntimeException.class, () -> {
            tareaProgramadaService.actualizarEstadoEmpleados();
        });

        verify(empleadoRepository, times(1)).desactivarEmpleadosConContratoExpirado();
    }
}