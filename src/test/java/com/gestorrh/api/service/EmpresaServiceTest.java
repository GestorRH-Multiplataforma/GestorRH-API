package com.gestorrh.api.service;

import com.gestorrh.api.dto.empresaDTO.PeticionRegistroEmpresaDTO;
import com.gestorrh.api.dto.empresaDTO.RespuestaEmpresaDTO;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpresaRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmpresaService empresaService;

    private PeticionRegistroEmpresaDTO peticionRegistro;

    @BeforeEach
    void setUp() {
        peticionRegistro = PeticionRegistroEmpresaDTO.builder()
                .email("test@empresa.com")
                .password("Clave123")
                .nombre("Empresa Test")
                .direccion("Calle Falsa 123")
                .telefono("123456789")
                .build();
    }

    private void simularAutenticacion(String email) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn(email);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    @DisplayName("Registrar empresa falla si el email ya existe")
    void registrarEmpresa_FallaEmailDuplicado() {
        when(empresaRepository.findByEmail(peticionRegistro.getEmail())).thenReturn(Optional.of(new Empresa()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> empresaService.registrarEmpresa(peticionRegistro));
        assertTrue(ex.getMessage().contains("Ya existe una empresa registrada"));
    }

    @Test
    @DisplayName("Registrar empresa con éxito encripta la contraseña")
    void registrarEmpresa_Exito() {
        when(empresaRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("claveEncriptada");

        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> {
            Empresa e = i.getArgument(0);
            e.setIdEmpresa(1L);
            return e;
        });

        RespuestaEmpresaDTO respuesta = empresaService.registrarEmpresa(peticionRegistro);

        assertNotNull(respuesta.getIdEmpresa());
        assertEquals(peticionRegistro.getEmail(), respuesta.getEmail());
        verify(passwordEncoder, times(1)).encode(peticionRegistro.getPassword());
    }

    @Test
    @DisplayName("Eliminar empresa borra la entidad correctamente")
    void eliminarMiEmpresa_Exito() {
        String emailAutenticado = "admin@empresa.com";
        simularAutenticacion(emailAutenticado);

        Empresa empresa = Empresa.builder().idEmpresa(1L).email(emailAutenticado).build();
        when(empresaRepository.findByEmail(emailAutenticado)).thenReturn(Optional.of(empresa));

        empresaService.eliminarMiEmpresa();

        verify(empresaRepository, times(1)).delete(empresa);
    }
}
