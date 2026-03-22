package com.gestorrh.api.service;

import com.gestorrh.api.dto.autenticacionDTO.RespuestaLoginDTO;
import com.gestorrh.api.dto.autenticacionDTO.PeticionLoginDTO;
import com.gestorrh.api.entity.Empleado;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import com.gestorrh.api.security.ServicioJwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio encargado de gestionar la lógica de autenticación (Login).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutenticacionService {

    private final EmpresaRepository empresaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder codificadorPassword;
    private final ServicioJwt servicioJwt;

    /**
     * Lógica para el login de una Empresa.
     */
    public RespuestaLoginDTO loginEmpresa(PeticionLoginDTO request) {

        Empresa empresa = empresaRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido (Empresa): El email '{}' no existe en la BD.", request.getEmail());
                    return new RuntimeException("Credenciales inválidas");
                });

        if (!codificadorPassword.matches(request.getPassword(), empresa.getPassword())) {
            log.warn("Intento de login fallido (Empresa): Contraseña incorrecta para el email '{}'.", request.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("rol", "EMPRESA");
        extraClaims.put("id", empresa.getIdEmpresa());

        String token = servicioJwt.generarToken(empresa.getEmail(), extraClaims);

        log.info("Inicio de sesión EXITOSO (Empresa): {}", empresa.getEmail());

        return RespuestaLoginDTO.builder()
                .token(token)
                .rol("EMPRESA")
                .id(empresa.getIdEmpresa())
                .nombre(empresa.getNombre())
                .build();
    }

    /**
     * Lógica para el login de un Empleado.
     */
    public RespuestaLoginDTO loginEmpleado(PeticionLoginDTO peticion) {

        Empleado empleado = empleadoRepository.findByEmail(peticion.getEmail())
                .orElseThrow(() -> {
                    log.warn("Intento de login fallido (Empleado): El email '{}' no existe en la BD.", peticion.getEmail());
                    return new RuntimeException("Credenciales inválidas");
                });

        if (!codificadorPassword.matches(peticion.getPassword(), empleado.getPassword())) {
            log.warn("Intento de login fallido (Empleado): Contraseña incorrecta para el email '{}'.", peticion.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        boolean contratoExpirado = empleado.getFechaBajaContrato() != null && !empleado.getFechaBajaContrato().isAfter(java.time.LocalDate.now());

        if (!empleado.getActivo() || contratoExpirado) {
            log.warn("Intento de login bloqueado (Empleado): La cuenta de '{}' está inactiva o su contrato ha expirado.", peticion.getEmail());
            throw new RuntimeException("Acceso denegado: Su relación laboral con la empresa ha finalizado.");
        }

        Map<String, Object> datosExtra = new HashMap<>();
        datosExtra.put("rol", empleado.getRol().name());
        datosExtra.put("id", empleado.getIdEmpleado());
        datosExtra.put("idEmpresa", empleado.getEmpresa().getIdEmpresa());

        String token = servicioJwt.generarToken(empleado.getEmail(), datosExtra);

        log.info("Inicio de sesión EXITOSO (Empleado): {} con Rol [{}]", empleado.getEmail(), empleado.getRol());

        return RespuestaLoginDTO.builder()
                .token(token)
                .rol(empleado.getRol().name())
                .id(empleado.getIdEmpleado())
                .nombre(empleado.getNombre() + " " + empleado.getApellidos())
                .build();
    }
}
