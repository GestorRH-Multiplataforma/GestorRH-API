package com.gestorrh.api.service;

import com.gestorrh.api.dto.empresa.*;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpresaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de la gestión integral del ciclo de vida de la entidad Empresa.
 * <p>
 * Centraliza funcionalidades críticas como el registro inicial, la gestión del perfil corporativo,
 * la configuración de parámetros de geovallado (Sede Central) y la gestión de la seguridad de la cuenta.
 * </p>
 * <p>
 * Garantiza que la configuración de la empresa sea coherente para que los empleados 
 * puedan operar correctamente con los módulos de fichaje y turnos.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder codificadorPassword;

    /**
     * Registra una nueva organización en la plataforma GestorRH.
     * <p>
     * El flujo de registro incluye la validación de disponibilidad del correo electrónico,
     * el cifrado de la contraseña de acceso y la persistencia de los datos de contacto iniciales.
     * </p>
     *
     * @param peticion Objeto {@link PeticionRegistroEmpresaDTO} con las credenciales y datos corporativos.
     * @return {@link RespuestaEmpresaDTO} con el perfil público de la empresa creada.
     * @throws RuntimeException Si el correo electrónico ya está en uso por otra empresa en el sistema.
     */
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

    /**
     * Recupera los datos del perfil de la empresa que ha iniciado sesión.
     *
     * @return RespuestaEmpresaDTO con la información detallada del perfil.
     */
    @Transactional(readOnly = true)
    public RespuestaEmpresaDTO obtenerMiPerfil() {
        Empresa empresa = obtenerEmpresaAutenticada();
        return mapearARespuesta(empresa);
    }

    /**
     * Actualiza la información corporativa y la configuración de geovallado de la empresa autenticada.
     * Permite definir la ubicación exacta de la sede y el radio de validez para los fichajes presenciales.
     *
     * @param peticion DTO con los nuevos datos de contacto y coordenadas GPS.
     * @return RespuestaEmpresaDTO con el perfil actualizado.
     */
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

    /**
     * Permite a la empresa cambiar su contraseña de acceso.
     * Valida que la contraseña actual proporcionada sea correcta antes de aplicar el cambio.
     *
     * @param peticion DTO con la contraseña actual y la nueva contraseña.
     * @throws RuntimeException Si la contraseña actual no coincide con la almacenada.
     */
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

    /**
     * Realiza el borrado definitivo de la cuenta de empresa y todos sus datos relacionados.
     * Esta operación es irreversible.
     */
    @Transactional
    public void eliminarMiEmpresa() {
        Empresa empresa = obtenerEmpresaAutenticada();
        empresaRepository.delete(empresa);
        log.warn("ATENCIÓN: La empresa '{}' ha ELIMINADO su cuenta y todos sus datos asociados.", empresa.getEmail());
    }

    /**
     * Recupera la entidad {@link Empresa} completa desde el repositorio basándose en la identidad del usuario autenticado.
     *
     * @return {@link Empresa} Entidad persistente de la empresa.
     * @throws EntityNotFoundException Si la empresa no existe en el sistema.
     */
    private Empresa obtenerEmpresaAutenticada() {
        String emailAuth = SecurityContextHolder.getContext().getAuthentication().getName();
        return empresaRepository.findByEmail(emailAuth)
                .orElseThrow(() -> new EntityNotFoundException("Error crítico: Empresa no encontrada en el sistema"));
    }

    /**
     * Transforma la entidad {@link Empresa} en un objeto de respuesta DTO para su exposición en la API.
     *
     * @param empresa Entidad a mapear.
     * @return RespuestaEmpresaDTO con los datos públicos del perfil.
     */
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
