package com.gestorrh.api.service;

import com.gestorrh.api.dto.fichaje.PeticionFichajeEntradaDTO;
import com.gestorrh.api.entity.AsignacionTurno;
import com.gestorrh.api.entity.Empleado;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.entity.Fichaje;
import com.gestorrh.api.entity.Turno;
import com.gestorrh.api.entity.enums.ModalidadTurno;
import com.gestorrh.api.repository.AsignacionTurnoRepository;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.FichajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FichajeServiceTest {

    @Mock private FichajeRepository fichajeRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private AsignacionTurnoRepository asignacionRepository;
    @Mock private GeofencingService geofencingService;

    @InjectMocks
    private FichajeService fichajeService;

    private Empleado empleadoPrueba;
    private Empresa empresaPrueba;

    @BeforeEach
    void setUp() {
        empresaPrueba = Empresa.builder()
                .idEmpresa(1L)
                .latitudSede(40.4168)
                .longitudSede(-3.7038)
                .radioValidez(100)
                .build();

        empleadoPrueba = Empleado.builder()
                .idEmpleado(1L)
                .email("empleado@test.com")
                .empresa(empresaPrueba)
                .build();

        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn("empleado@test.com");
        SecurityContextHolder.setContext(ctx);

        lenient().when(empleadoRepository.findByEmail("empleado@test.com")).thenReturn(Optional.of(empleadoPrueba));
    }

    @Test
    @DisplayName("Falla al fichar entrada si ya hay un turno abierto")
    void ficharEntrada_FallaDobleEntrada() {
        PeticionFichajeEntradaDTO peticion = new PeticionFichajeEntradaDTO(40.4168, -3.7038);
        when(fichajeRepository.findByEmpleadoIdEmpleadoAndFechaAndHoraSalidaIsNull(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(new Fichaje()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> fichajeService.ficharEntrada(peticion));
        assertTrue(ex.getMessage().contains("ya tienes un turno abierto"));
    }

    @Test
    @DisplayName("Falla al fichar entrada presencial si el GPS no es válido")
    void ficharEntrada_FallaGeofencing() {
        PeticionFichajeEntradaDTO peticion = new PeticionFichajeEntradaDTO(40.8000, -3.9000);

        Turno turno = Turno.builder().horaInicio(LocalTime.of(8, 0)).build();
        AsignacionTurno asignacion = AsignacionTurno.builder().turno(turno).modalidad(ModalidadTurno.PRESENCIAL).build();

        when(fichajeRepository.findByEmpleadoIdEmpleadoAndFechaAndHoraSalidaIsNull(anyLong(), any())).thenReturn(Collections.emptyList());
        when(asignacionRepository.findByEmpleadoIdEmpleadoAndFecha(anyLong(), any())).thenReturn(List.of(asignacion));
        when(geofencingService.esFichajeValido(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> fichajeService.ficharEntrada(peticion));
        assertTrue(ex.getMessage().contains("fuera del radio permitido"));
    }
}
