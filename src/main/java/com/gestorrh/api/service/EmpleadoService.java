package com.gestorrh.api.service;

import com.gestorrh.api.dto.empleadoDTO.*;
import com.gestorrh.api.entity.Empleado;
import com.gestorrh.api.entity.Empresa;
import com.gestorrh.api.repository.EmpleadoRepository;
import com.gestorrh.api.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la lógica de negocio de los Empleados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
            log.warn("Empresa '{}' intentó crear empleado fallido: El correo '{}' ya existe.", correoEmpresaAuth, peticion.getEmail());
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

        log.info("ALTA DE EMPLEADO: La empresa '{}' ha registrado a '{}' con rol [{}]", empresa.getEmail(), nuevoEmpleado.getEmail(), nuevoEmpleado.getRol());

        return RespuestaCrearEmpleadoDTO.builder()
                .idEmpleado(nuevoEmpleado.getIdEmpleado())
                .nombre(nuevoEmpleado.getNombre())
                .apellidos(nuevoEmpleado.getApellidos())
                .email(nuevoEmpleado.getEmail())
                .rol(nuevoEmpleado.getRol())
                .passwordGenerada(contrasenaPlana)
                .build();
    }

    /**
     * Obtiene la lista de todos los empleados pertenecientes a la empresa autenticada.
     */
    @Transactional(readOnly = true)
    public List<RespuestaEmpleadoDTO> obtenerEmpleadosDeEmpresa() {

        String correoEmpresaAuth = SecurityContextHolder.getContext().getAuthentication().getName();

        Empresa empresa = empresaRepository.findByEmail(correoEmpresaAuth)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        List<Empleado> empleados = empleadoRepository.findByEmpresaIdEmpresa(empresa.getIdEmpresa());

        return empleados.stream().map(emp -> {
            boolean esRealmenteActivo = emp.getActivo() &&
                    (emp.getFechaBajaContrato() == null || emp.getFechaBajaContrato().isAfter(LocalDate.now()));

            return RespuestaEmpleadoDTO.builder()
                    .idEmpleado(emp.getIdEmpleado())
                    .email(emp.getEmail())
                    .nombre(emp.getNombre())
                    .apellidos(emp.getApellidos())
                    .telefono(emp.getTelefono())
                    .puesto(emp.getPuesto())
                    .departamento(emp.getDepartamento())
                    .rol(emp.getRol())
                    .activo(emp.getActivo())
                    .fechaBajaContrato(emp.getFechaBajaContrato())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Actualiza los datos de un empleado, verificando que pertenezca a la empresa logueada.
     */
    @Transactional
    public RespuestaEmpleadoDTO actualizarEmpleado(Long idEmpleado, PeticionActualizarEmpleadoDTO peticion) {

        String correoEmpresaAuth = SecurityContextHolder.getContext().getAuthentication().getName();
        Empresa empresaLogueada = empresaRepository.findByEmail(correoEmpresaAuth)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + idEmpleado));

        if (!empleado.getEmpresa().getIdEmpresa().equals(empresaLogueada.getIdEmpresa())) {
            log.warn("VIOLACIÓN DE SEGURIDAD: La empresa '{}' intentó modificar al empleado ID {}, que pertenece a otra empresa.", correoEmpresaAuth, idEmpleado);
            throw new RuntimeException("Acceso denegado: Este empleado no pertenece a tu empresa.");
        }

        empleado.setNombre(peticion.getNombre());
        empleado.setApellidos(peticion.getApellidos());
        empleado.setTelefono(peticion.getTelefono());
        empleado.setPuesto(peticion.getPuesto());
        empleado.setDepartamento(peticion.getDepartamento());
        empleado.setRol(peticion.getRol());

        empleado = empleadoRepository.save(empleado);

        log.info("La empresa '{}' ha ACTUALIZADO los datos del empleado ID {} ({})", correoEmpresaAuth, idEmpleado, empleado.getEmail());

        boolean esRealmenteActivo = empleado.getActivo() &&
                (empleado.getFechaBajaContrato() == null || empleado.getFechaBajaContrato().isAfter(LocalDate.now()));

        return RespuestaEmpleadoDTO.builder()
                .idEmpleado(empleado.getIdEmpleado())
                .email(empleado.getEmail())
                .nombre(empleado.getNombre())
                .apellidos(empleado.getApellidos())
                .telefono(empleado.getTelefono())
                .puesto(empleado.getPuesto())
                .departamento(empleado.getDepartamento())
                .rol(empleado.getRol())
                .activo(esRealmenteActivo)
                .fechaBajaContrato(empleado.getFechaBajaContrato())
                .build();
    }

    /**
     * Registra la baja de un empleado poniendo la fecha de hoy como fin de contrato.
     */
    @Transactional
    public void darDeBajaEmpleado(Long idEmpleado, LocalDate fechaBaja) {
        String correoEmpresaAuth = SecurityContextHolder.getContext().getAuthentication().getName();
        Empresa empresaLogueada = empresaRepository.findByEmail(correoEmpresaAuth)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        if (!empleado.getEmpresa().getIdEmpresa().equals(empresaLogueada.getIdEmpresa())) {
            log.warn("VIOLACIÓN DE SEGURIDAD: La empresa '{}' intentó dar de baja al empleado ID {}, que pertenece a otra empresa.", correoEmpresaAuth, idEmpleado);
            throw new RuntimeException("No tienes permiso para dar de baja a este empleado");
        }

        empleado.setFechaBajaContrato(fechaBaja);

        if (!fechaBaja.isAfter(LocalDate.now())) {
            empleado.setActivo(false);
            log.info("La empresa '{}' ha tramitado la BAJA INMEDIATA del empleado ID {} ({})", correoEmpresaAuth, idEmpleado, empleado.getEmail());
        } else {
            log.info("La empresa '{}' ha programado la BAJA del empleado ID {} ({}) para la fecha: {}", correoEmpresaAuth, idEmpleado, empleado.getEmail(), fechaBaja);
        }

        empleadoRepository.save(empleado);
    }

    /**
     * Readmite a un empleado que estaba de baja, reseteando su fecha de fin de contrato
     * y generándole una nueva contraseña de acceso.
     */
    @Transactional
    public RespuestaCrearEmpleadoDTO readmitirEmpleado(Long idEmpleado) {

        String correoEmpresaAuth = SecurityContextHolder.getContext().getAuthentication().getName();
        Empresa empresaLogueada = empresaRepository.findByEmail(correoEmpresaAuth)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Empleado empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + idEmpleado));

        if (!empleado.getEmpresa().getIdEmpresa().equals(empresaLogueada.getIdEmpresa())) {
            log.warn("VIOLACIÓN DE SEGURIDAD: La empresa '{}' intentó readmitir al empleado ID {}, que pertenece a otra empresa.", correoEmpresaAuth, idEmpleado);
            throw new RuntimeException("Acceso denegado: Este empleado no pertenece a tu empresa.");
        }

        if (empleado.getFechaBajaContrato() == null) {
            log.warn("La empresa '{}' intentó readmitir al empleado ID {}, pero ya estaba en estado activo.", correoEmpresaAuth, idEmpleado);
            throw new RuntimeException("El empleado ya está activo. No se puede readmitir a alguien que no está de baja.");
        }

        String nuevaContrasenaPlana = java.util.UUID.randomUUID().toString().substring(0, 8);
        String contrasenaEncriptada = codificadorPassword.encode(nuevaContrasenaPlana);

        empleado.setFechaBajaContrato(null);
        empleado.setActivo(true);
        empleado.setPassword(contrasenaEncriptada);

        empleado = empleadoRepository.save(empleado);

        log.info("La empresa '{}' ha READMITIDO al empleado ID {} ({}) y se ha generado una nueva contraseña.", correoEmpresaAuth, idEmpleado, empleado.getEmail());

        return RespuestaCrearEmpleadoDTO.builder()
                .idEmpleado(empleado.getIdEmpleado())
                .nombre(empleado.getNombre())
                .apellidos(empleado.getApellidos())
                .email(empleado.getEmail())
                .rol(empleado.getRol())
                .passwordGenerada(nuevaContrasenaPlana)
                .build();
    }

    /**
     * Obtiene los datos del empleado que ha iniciado sesión.
     * Se basa exclusivamente en el Token JWT, ignorando cualquier ID externo.
     */
    @Transactional(readOnly = true)
    public RespuestaEmpleadoDTO obtenerMiPerfil() {
        String correoEmpleadoAuth = SecurityContextHolder.getContext().getAuthentication().getName();

        Empleado empleado = empleadoRepository.findByEmail(correoEmpleadoAuth)
                .orElseThrow(() -> new RuntimeException("Error crítico: Empleado no encontrado en el sistema"));

        boolean esRealmenteActivo = empleado.getActivo() &&
                (empleado.getFechaBajaContrato() == null || empleado.getFechaBajaContrato().isAfter(LocalDate.now()));

        return RespuestaEmpleadoDTO.builder()
                .idEmpleado(empleado.getIdEmpleado())
                .email(empleado.getEmail())
                .nombre(empleado.getNombre())
                .apellidos(empleado.getApellidos())
                .telefono(empleado.getTelefono())
                .puesto(empleado.getPuesto())
                .departamento(empleado.getDepartamento())
                .rol(empleado.getRol())
                .activo(esRealmenteActivo)
                .fechaBajaContrato(empleado.getFechaBajaContrato())
                .build();
    }

    /**
     * Permite al empleado autenticado cambiar su propia contraseña.
     */
    @Transactional
    public void cambiarMiContrasena(PeticionCambiarPasswordDTO peticion) {
        String correoEmpleadoAuth = SecurityContextHolder.getContext().getAuthentication().getName();

        Empleado empleado = empleadoRepository.findByEmail(correoEmpleadoAuth)
                .orElseThrow(() -> new RuntimeException("Error crítico: Empleado no encontrado en el sistema"));

        if (!codificadorPassword.matches(peticion.getPasswordActual(), empleado.getPassword())) {
            log.warn("Cambio de contraseña DENEGADO para el empleado '{}': La contraseña actual no coincide.", correoEmpleadoAuth);
            throw new RuntimeException("La contraseña actual no es correcta. Operación denegada.");
        }

        String nuevaContrasenaEncriptada = codificadorPassword.encode(peticion.getNuevaPassword());
        empleado.setPassword(nuevaContrasenaEncriptada);

        empleadoRepository.save(empleado);
        log.info("El empleado '{}' ha cambiado su contraseña con éxito.", correoEmpleadoAuth);
    }
}