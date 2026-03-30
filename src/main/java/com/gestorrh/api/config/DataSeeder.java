package com.gestorrh.api.config;

import com.gestorrh.api.entity.*;
import com.gestorrh.api.entity.enums.*;
import com.gestorrh.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Clase encargada de inyectar el ecosistema de pruebas completo al arrancar la aplicación
 * si la base de datos está vacía. Genera un escenario multi-tenant con turnos, fichajes y ausencias.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final EmpresaRepository empresaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TurnoRepository turnoRepository;
    private final AsignacionTurnoRepository asignacionRepository;
    private final FichajeRepository fichajeRepository;
    private final AusenciaRepository ausenciaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (empresaRepository.count() == 0) {
            log.info("[DataSeeder] Iniciando inyección de datos de prueba...");

            List<Empresa> empresas = crearEmpresas();
            Empresa empresaPrincipal = empresas.get(0);

            List<Empleado> empleados = crearEmpleados(empresaPrincipal);
            Empleado supervisor = empleados.get(0);
            Empleado empleadoBase = empleados.get(1);

            List<Turno> turnos = crearTurnos(empresaPrincipal);

            crearAsignacionesYFichajes(empleadoBase, turnos.get(0), turnos.get(1));

            crearAusencias(empleadoBase, supervisor);

            log.info("[DataSeeder] ¡Ecosistema de pruebas inyectado con éxito!");
            log.info("Credenciales de prueba (Password para todos: 123456):");
            log.info("   - EMPRESA    (Login Empresa) : admin@tech.com");
            log.info("   - SUPERVISOR (Login Empleado): super@tech.com");
            log.info("   - EMPLEADO   (Login Empleado): empleado@tech.com");
        }
    }

    private List<Empresa> crearEmpresas() {
        Empresa techSolutions = Empresa.builder()
                .email("admin@tech.com")
                .password(passwordEncoder.encode("123456"))
                .nombre("Tech Solutions S.L.")
                .direccion("Calle Falsa 123, Madrid")
                .telefono("912345678")
                .latitudSede(40.4168)
                .longitudSede(-3.7038)
                .radioValidez(500)
                .build();

        Empresa empresaFantasma = Empresa.builder()
                .email("admin@fantasma.com")
                .password(passwordEncoder.encode("123456"))
                .nombre("Empresa Fantasma S.L.")
                .direccion("Av. Olvido 404")
                .telefono("000000000")
                .build();

        return empresaRepository.saveAll(List.of(techSolutions, empresaFantasma));
    }

    private List<Empleado> crearEmpleados(Empresa empresa) {
        Empleado supervisor = Empleado.builder()
                .empresa(empresa)
                .email("super@tech.com")
                .password(passwordEncoder.encode("123456"))
                .nombre("Carlos")
                .apellidos("Jefe")
                .puesto("Team Lead IT")
                .departamento("IT")
                .rol(RolEmpleado.SUPERVISOR)
                .activo(true)
                .build();

        Empleado empleado = Empleado.builder()
                .empresa(empresa)
                .email("empleado@tech.com")
                .password(passwordEncoder.encode("123456"))
                .nombre("Juan")
                .apellidos("Pérez")
                .puesto("Desarrollador Junior")
                .departamento("IT")
                .rol(RolEmpleado.EMPLEADO)
                .activo(true)
                .build();

        return empleadoRepository.saveAll(List.of(supervisor, empleado));
    }

    private List<Turno> crearTurnos(Empresa empresa) {
        Turno manana = Turno.builder()
                .empresa(empresa)
                .descripcion("Turno de Mañana")
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(16, 0))
                .build();

        Turno tarde = Turno.builder()
                .empresa(empresa)
                .descripcion("Turno de Tarde")
                .horaInicio(LocalTime.of(16, 0))
                .horaFin(LocalTime.of(0, 0))
                .build();

        return turnoRepository.saveAll(List.of(manana, tarde));
    }

    private void crearAsignacionesYFichajes(Empleado empleado, Turno turnoManana, Turno turnoTarde) {
        LocalDate hoy = LocalDate.now();

        AsignacionTurno asigAyer = AsignacionTurno.builder()
                .empleado(empleado)
                .turno(turnoManana)
                .fecha(hoy.minusDays(1))
                .modalidad(ModalidadTurno.PRESENCIAL)
                .build();
        asignacionRepository.save(asigAyer);

        Fichaje fichajeAyer = Fichaje.builder()
                .empleado(empleado)
                .asignacion(asigAyer)
                .fecha(hoy.minusDays(1))
                .horaEntrada(hoy.minusDays(1).atTime(8, 0))
                .latitudEntrada(40.4168).longitudEntrada(-3.7038)
                .horaSalida(hoy.minusDays(1).atTime(16, 5))
                .latitudSalida(40.4168).longitudSalida(-3.7038)
                .build();
        fichajeRepository.save(fichajeAyer);

        AsignacionTurno asigHoy = AsignacionTurno.builder()
                .empleado(empleado)
                .turno(turnoManana)
                .fecha(hoy)
                .modalidad(ModalidadTurno.PRESENCIAL)
                .build();
        asignacionRepository.save(asigHoy);

        Fichaje fichajeHoy = Fichaje.builder()
                .empleado(empleado)
                .asignacion(asigHoy)
                .fecha(hoy)
                .horaEntrada(hoy.atTime(8, 25))
                .latitudEntrada(40.4168).longitudEntrada(-3.7038)
                .incidencias("Retraso en la entrada.")
                .build();
        fichajeRepository.save(fichajeHoy);

        AsignacionTurno asigManana = AsignacionTurno.builder()
                .empleado(empleado)
                .turno(turnoTarde)
                .fecha(hoy.plusDays(1))
                .modalidad(ModalidadTurno.TELETRABAJO)
                .build();
        asignacionRepository.save(asigManana);
    }

    private void crearAusencias(Empleado empleado, Empleado supervisor) {
        LocalDate hoy = LocalDate.now();

        Ausencia vacaciones = Ausencia.builder()
                .empleado(empleado)
                .tipo(TipoAusencia.VACACIONES)
                .descripcion("Vacaciones de verano")
                .fechaInicio(hoy.minusDays(20))
                .fechaFin(hoy.minusDays(15))
                .estado(EstadoAusencia.APROBADA)
                .responsableRevision(supervisor.getNombre() + " " + supervisor.getApellidos())
                .build();

        Ausencia bajaMedica = Ausencia.builder()
                .empleado(empleado)
                .tipo(TipoAusencia.MEDICA)
                .descripcion("Operación médica programada")
                .fechaInicio(hoy.plusDays(5))
                .fechaFin(hoy.plusDays(10))
                .estado(EstadoAusencia.SOLICITADA)
                .build();

        ausenciaRepository.saveAll(List.of(vacaciones, bajaMedica));
    }
}
