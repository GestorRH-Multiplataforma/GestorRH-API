package com.gestorrh.api.service;

import com.gestorrh.api.dto.empresaDTO.*;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión completa del ciclo de vida de la Empresa.
 * Épica E2: Registro, Perfil y configuración de Sede (Geofencing).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder codificadorPassword;

    @Transactional
    public RespuestaEmpresaDTO registrarEmpresa(PeticionRegistroEmpresaDTO peticion) {
        if (empresaRepository.findByEmail(peticion.getEmail()).isPresent()) {
            log.warn("Intento de registro fallido: El email '{}' ya está registrado por otra empresa.", peticion.getEmail());
            throw new RuntimeException("Ya existe una empresa registrada con este correo.");
        }

        Empresa nuevaEmpresa = Empresa.builder()
                .email(peticion.getEmail())
                .password(codificadorPassword.encode(peticion.getPassword()))
                .nombre(peticion.getNombre())
                .direccion(peticion.getDireccion())
                .telefono(peticion.getTelefono())
                .build();

        nuevaEmpresa = empresaRepository.save(nuevaEmpresa);

        log.info("NUEVA EMPRESA REGISTRADA: {} (Email: {})", nuevaEmpresa.getNombre(), nuevaEmpresa.getEmail());

        return mapearARespuesta(nuevaEmpresa);
    }

    @Transactional(readOnly = true)
    public RespuestaEmpresaDTO obtenerMiPerfil() {
        Empresa empresa = obtenerEmpresaAutenticada();
        return mapearARespuesta(empresa);
    }

    @Transactional
    public RespuestaEmpresaDTO actualizarMiPerfil(PeticionActualizarEmpresaDTO peticion) {
        Empresa empresa = obtenerEmpresaAutenticada();

        empresa.setNombre(peticion.getNombre());
        empresa.setDireccion(peticion.getDireccion());
        empresa.setTelefono(peticion.getTelefono());

        empresa.setLatitudSede(peticion.getLatitudSede());
        empresa.setLongitudSede(peticion.getLongitudSede());
        empresa.setRadioValidez(peticion.getRadioValidez());

        empresa = empresaRepository.save(empresa);

        log.info("La empresa '{}' ha ACTUALIZADO su perfil y/o configuración de geovallado.", empresa.getEmail());

        return mapearARespuesta(empresa);
    }

    @Transactional
    public void cambiarMiContrasena(PeticionCambiarPasswordEmpresaDTO peticion) {
        Empresa empresa = obtenerEmpresaAutenticada();

        if (!codificadorPassword.matches(peticion.getPasswordActual(), empresa.getPassword())) {
            log.warn("Cambio de contraseña DENEGADO para la empresa '{}': La contraseña actual no coincide.", empresa.getEmail());
            throw new RuntimeException("La contraseña actual no es correcta. Operación denegada.");
        }

        empresa.setPassword(codificadorPassword.encode(peticion.getNuevaPassword()));
        empresaRepository.save(empresa);

        log.info("La empresa '{}' ha cambiado su contraseña con éxito.", empresa.getEmail());
    }

    @Transactional
    public void eliminarMiEmpresa() {
        Empresa empresa = obtenerEmpresaAutenticada();
        empresaRepository.delete(empresa);
        log.warn("ATENCIÓN: La empresa '{}' ha ELIMINADO su cuenta y todos sus datos asociados.", empresa.getEmail());
    }

    // MÉTODOS PRIVADOS
    private Empresa obtenerEmpresaAutenticada() {
        String emailAuth = SecurityContextHolder.getContext().getAuthentication().getName();
        return empresaRepository.findByEmail(emailAuth)
                .orElseThrow(() -> new RuntimeException("Error crítico: Empresa no encontrada en el sistema"));
    }

    private RespuestaEmpresaDTO mapearARespuesta(Empresa empresa) {
        return RespuestaEmpresaDTO.builder()
                .idEmpresa(empresa.getIdEmpresa())
                .email(empresa.getEmail())
                .nombre(empresa.getNombre())
                .direccion(empresa.getDireccion())
                .telefono(empresa.getTelefono())
                .latitudSede(empresa.getLatitudSede())
                .longitudSede(empresa.getLongitudSede())
                .radioValidez(empresa.getRadioValidez())
                .build();
    }
}
