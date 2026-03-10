package com.gestorrh.api.service;

import com.gestorrh.api.dto.PeticionCrearEmpleadoDTO;
import com.gestorrh.api.dto.RespuestaCrearEmpleadoDTO;
import com.gestorrh.api.entity.Empleado;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio encargado de la lógica de negocio de los Empleados.
 */
@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder codificadorPassword;

    /**
     * Crea un nuevo empleado asociado automáticamente a la empresa que hace la petición.
     */
    @Transactional
    public RespuestaCrearEmpleadoDTO crearEmpleado(PeticionCrearEmpleadoDTO peticion) {

        String correoEmpresaAuth = SecurityContextHolder.getContext().getAuthentication().getName();

        Empresa empresa = empresaRepository.findByEmail(correoEmpresaAuth)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada en el sistema"));

        if (empleadoRepository.findByEmail(peticion.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con el correo: " + peticion.getEmail());
        }

        String contrasenaPlana = UUID.randomUUID().toString().substring(0, 8);

        String contrasenaEncriptada = codificadorPassword.encode(contrasenaPlana);

        Empleado nuevoEmpleado = Empleado.builder()
                .empresa(empresa)
                .email(peticion.getEmail())
                .password(contrasenaEncriptada)
                .nombre(peticion.getNombre())
                .apellidos(peticion.getApellidos())
                .telefono(peticion.getTelefono())
                .puesto(peticion.getPuesto())
                .departamento(peticion.getDepartamento())
                .rol(peticion.getRol())
                .activo(true)
                .build();

        nuevoEmpleado = empleadoRepository.save(nuevoEmpleado);

        return RespuestaCrearEmpleadoDTO.builder()
                .idEmpleado(nuevoEmpleado.getIdEmpleado())
                .nombre(nuevoEmpleado.getNombre())
                .apellidos(nuevoEmpleado.getApellidos())
                .email(nuevoEmpleado.getEmail())
                .rol(nuevoEmpleado.getRol())
                .passwordGenerada(contrasenaPlana)
                .build();
    }
}