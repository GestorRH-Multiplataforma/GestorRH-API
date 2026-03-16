package com.gestorrh.api.service;

import com.gestorrh.api.dto.empresaDTO.*;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
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
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder codificadorPassword;

    @Transactional
    public RespuestaEmpresaDTO registrarEmpresa(PeticionRegistroEmpresaDTO peticion) {
        if (empresaRepository.findByEmail(peticion.getEmail()).isPresent()) {
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
        return mapearARespuesta(empresa);
    }

    @Transactional
    public void cambiarMiContrasena(PeticionCambiarPasswordEmpresaDTO peticion) {
        Empresa empresa = obtenerEmpresaAutenticada();

        if (!codificadorPassword.matches(peticion.getPasswordActual(), empresa.getPassword())) {
            throw new RuntimeException("La contraseña actual no es correcta. Operación denegada.");
        }

        empresa.setPassword(codificadorPassword.encode(peticion.getNuevaPassword()));
        empresaRepository.save(empresa);
    }

    @Transactional
    public void eliminarMiEmpresa() {
        Empresa empresa = obtenerEmpresaAutenticada();
        empresaRepository.delete(empresa);
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
