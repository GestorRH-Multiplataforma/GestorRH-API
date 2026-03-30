package com.gestorrh.api.service;

import com.gestorrh.api.dto.empresa.*;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpresaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private PasswordEncoder codificadorPassword;

    @InjectMocks
    private EmpresaService empresaService;

    private Empresa empresaEjemplo;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        empresaEjemplo = Empresa.builder()
                .idEmpresa(1L)
                .email("test@empresa.com")
                .password("password123")
                .nombre("Empresa Test")
                .direccion("Calle Falsa 123")
                .telefono("600000000")
                .latitudSede(40.4168)
                .longitudSede(-3.7038)
                .radioValidez(500)
                .build();

        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuth(String email) {
        lenient().when(authentication.getName()).thenReturn(email);
    }

    @Nested
    @DisplayName("Tests para registrarEmpresa")
    class RegistrarEmpresaTests {

        @Test
        @DisplayName("Éxito: Registro de empresa correcto")
        void registrarEmpresa_Exito() {
            PeticionRegistroEmpresaDTO peticion = PeticionRegistroEmpresaDTO.builder()
                    .email("nueva@empresa.com")
                    .password("secret123")
                    .nombre("Nueva Empresa")
                    .direccion("Calle Nueva 1")
                    .telefono("611111111")
                    .build();

            when(empresaRepository.findByEmail(peticion.getEmail())).thenReturn(Optional.empty());
            when(codificadorPassword.encode(peticion.getPassword())).thenReturn("encodedSecret");
            when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
                Empresa e = invocation.getArgument(0);
                e.setIdEmpresa(2L);
                return e;
            });

            RespuestaEmpresaDTO respuesta = empresaService.registrarEmpresa(peticion);

            assertNotNull(respuesta);
            assertEquals(peticion.getEmail(), respuesta.getEmail());
            assertEquals(peticion.getNombre(), respuesta.getNombre());
            assertEquals(2L, respuesta.getIdEmpresa());
            verify(empresaRepository).save(any(Empresa.class));
        }

        @Test
        @DisplayName("Fallo: Email ya registrado")
        void registrarEmpresa_EmailDuplicado() {
            PeticionRegistroEmpresaDTO peticion = PeticionRegistroEmpresaDTO.builder()
                    .email("test@empresa.com")
                    .build();

            when(empresaRepository.findByEmail(peticion.getEmail())).thenReturn(Optional.of(empresaEjemplo));

            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                empresaService.registrarEmpresa(peticion)
            );

            assertEquals("Ya existe una empresa registrada con este correo.", exception.getMessage());
            verify(empresaRepository, never()).save(any(Empresa.class));
        }
    }

    @Nested
    @DisplayName("Tests para obtenerMiPerfil")
    class ObtenerMiPerfilTests {

        @Test
        @DisplayName("Éxito: Obtener perfil de empresa autenticada")
        void obtenerMiPerfil_Exito() {
            mockAuth(empresaEjemplo.getEmail());
            when(empresaRepository.findByEmail(empresaEjemplo.getEmail())).thenReturn(Optional.of(empresaEjemplo));

            RespuestaEmpresaDTO respuesta = empresaService.obtenerMiPerfil();

            assertNotNull(respuesta);
            assertEquals(empresaEjemplo.getEmail(), respuesta.getEmail());
            assertEquals(empresaEjemplo.getNombre(), respuesta.getNombre());
        }

        @Test
        @DisplayName("Fallo: Empresa no encontrada en el sistema")
        void obtenerMiPerfil_NoEncontrada() {
            mockAuth("desconocido@empresa.com");
            when(empresaRepository.findByEmail("desconocido@empresa.com")).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> 
                empresaService.obtenerMiPerfil()
            );

            assertEquals("Error crítico: Empresa no encontrada en el sistema", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Tests para actualizarMiPerfil")
    class ActualizarMiPerfilTests {

        @Test
        @DisplayName("Éxito: Actualizar datos de la empresa")
        void actualizarMiPerfil_Exito() {
            mockAuth(empresaEjemplo.getEmail());
            when(empresaRepository.findByEmail(empresaEjemplo.getEmail())).thenReturn(Optional.of(empresaEjemplo));
            
            PeticionActualizarEmpresaDTO peticion = PeticionActualizarEmpresaDTO.builder()
                    .nombre("Nombre Actualizado")
                    .direccion("Direccion Nueva")
                    .telefono("999999999")
                    .latitudSede(10.0)
                    .longitudSede(20.0)
                    .radioValidez(1000)
                    .build();

            when(empresaRepository.save(any(Empresa.class))).thenReturn(empresaEjemplo);

            RespuestaEmpresaDTO respuesta = empresaService.actualizarMiPerfil(peticion);

            assertNotNull(respuesta);
            assertEquals("Nombre Actualizado", empresaEjemplo.getNombre());
            assertEquals("Direccion Nueva", empresaEjemplo.getDireccion());
            assertEquals(10.0, empresaEjemplo.getLatitudSede());
            verify(empresaRepository).save(empresaEjemplo);
        }
    }

    @Nested
    @DisplayName("Tests para cambiarMiContrasena")
    class CambiarMiContrasenaTests {

        @Test
        @DisplayName("Éxito: Cambio de contraseña correcto")
        void cambiarMiContrasena_Exito() {
            mockAuth(empresaEjemplo.getEmail());
            when(empresaRepository.findByEmail(empresaEjemplo.getEmail())).thenReturn(Optional.of(empresaEjemplo));
            
            PeticionCambiarPasswordEmpresaDTO peticion = PeticionCambiarPasswordEmpresaDTO.builder()
                    .passwordActual("password123")
                    .nuevaPassword("newPassword456")
                    .build();

            when(codificadorPassword.matches(peticion.getPasswordActual(), empresaEjemplo.getPassword())).thenReturn(true);
            when(codificadorPassword.encode(peticion.getNuevaPassword())).thenReturn("encodedNewPassword");

            assertDoesNotThrow(() -> empresaService.cambiarMiContrasena(peticion));

            assertEquals("encodedNewPassword", empresaEjemplo.getPassword());
            verify(empresaRepository).save(empresaEjemplo);
        }

        @Test
        @DisplayName("Fallo: Contraseña actual incorrecta")
        void cambiarMiContrasena_PasswordIncorrecta() {
            mockAuth(empresaEjemplo.getEmail());
            when(empresaRepository.findByEmail(empresaEjemplo.getEmail())).thenReturn(Optional.of(empresaEjemplo));
            
            PeticionCambiarPasswordEmpresaDTO peticion = PeticionCambiarPasswordEmpresaDTO.builder()
                    .passwordActual("erronea")
                    .nuevaPassword("newPassword")
                    .build();

            when(codificadorPassword.matches(anyString(), anyString())).thenReturn(false);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                empresaService.cambiarMiContrasena(peticion)
            );

            assertEquals("La contraseña actual no es correcta. Operación denegada.", exception.getMessage());
            verify(empresaRepository, never()).save(any(Empresa.class));
        }
    }

    @Nested
    @DisplayName("Tests para eliminarMiEmpresa")
    class EliminarMiEmpresaTests {

        @Test
        @DisplayName("Éxito: Eliminar empresa autenticada")
        void eliminarMiEmpresa_Exito() {
            mockAuth(empresaEjemplo.getEmail());
            when(empresaRepository.findByEmail(empresaEjemplo.getEmail())).thenReturn(Optional.of(empresaEjemplo));

            assertDoesNotThrow(() -> empresaService.eliminarMiEmpresa());

            verify(empresaRepository).delete(empresaEjemplo);
        }
    }
}
