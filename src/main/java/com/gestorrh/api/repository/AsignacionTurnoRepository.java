package com.gestorrh.api.repository;

import com.gestorrh.api.entity.AsignacionTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para gestionar las operaciones de base de datos de la entidad {@link AsignacionTurno}.
 * Se encarga de la persistencia de las planificaciones de turnos para los empleados.
 */
@Repository
public interface AsignacionTurnoRepository extends JpaRepository<AsignacionTurno, Long> {

    /**
     * Busca las asignaciones de turno de un empleado para una fecha concreta.
     * @param idEmpleado El ID del empleado.
     * @param fecha La fecha a consultar.
     * @return Lista de asignaciones de turno encontradas.
     */
    List<AsignacionTurno> findByEmpleadoIdEmpleadoAndFecha(Long idEmpleado, LocalDate fecha);

    /**
     * Busca las asignaciones de turno para una empresa y departamento específicos.
     * <p>
     * Utiliza un doble JOIN FETCH para traer al empleado y su turno asignado
     * en una sola consulta SQL, eliminando el problema N+1 múltiple.
     * </p>
     * @param idEmpresa El ID de la empresa.
     * @param departamento El nombre del departamento (ignora mayúsculas/minúsculas).
     * @return Lista de asignaciones para ese departamento.
     */
    @Query("SELECT a FROM AsignacionTurno a " +
            "JOIN FETCH a.empleado e " +
            "JOIN FETCH a.turno t " +
            "WHERE e.empresa.idEmpresa = :idEmpresa " +
            "AND LOWER(e.departamento) = LOWER(:departamento)")
    List<AsignacionTurno> findByEmpleadoEmpresaIdEmpresaAndEmpleadoDepartamentoIgnoreCase(
            @Param("idEmpresa") Long idEmpresa,
            @Param("departamento") String departamento);

    /**
     * Recupera todas las asignaciones de turno de una empresa.
     * @param idEmpresa El ID de la empresa.
     * @return Lista de asignaciones de la empresa.
     */
    List<AsignacionTurno> findByEmpleadoEmpresaIdEmpresa(Long idEmpresa);

    /**
     * Recupera todas las asignaciones de turno de un empleado.
     * @param idEmpleado El ID del empleado.
     * @return Lista de asignaciones del empleado.
     */
    List<AsignacionTurno> findByEmpleadoIdEmpleado(Long idEmpleado);

    /**
     * Busca las asignaciones de turno de un empleado en un rango de fechas determinado.
     * <p>
     * Utiliza JOIN FETCH para cargar el turno y el empleado asociados,
     * evitando latencia y consultas N+1 al renderizar el calendario mensual.
     * </p>
     * @param idEmpleado El ID del empleado.
     * @param fechaInicio Fecha inicial del rango.
     * @param fechaFin Fecha final del rango.
     * @return Lista de asignaciones en el periodo indicado.
     */
    @Query("SELECT a FROM AsignacionTurno a " +
            "JOIN FETCH a.turno t " +
            "JOIN FETCH a.empleado e " +
            "WHERE e.idEmpleado = :idEmpleado " +
            "AND a.fecha BETWEEN :fechaInicio AND :fechaFin")
    List<AsignacionTurno> findByEmpleadoIdEmpleadoAndFechaBetween(
            @Param("idEmpleado") Long idEmpleado,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    /**
     * Cuenta el número de empleados distintos que tienen un turno planificado para el día de hoy.
     * @param idEmpresa El ID de la empresa.
     * @param fechaHoy La fecha actual.
     * @return Número total de empleados planificados hoy.
     */
    @Query("SELECT COUNT(DISTINCT a.empleado.idEmpleado) " +
            "FROM AsignacionTurno a " +
            "WHERE a.empleado.empresa.idEmpresa = :idEmpresa " +
            "AND a.fecha = :fechaHoy")
    Long contarEmpleadosPlanificadosHoy(@Param("idEmpresa") Long idEmpresa, @Param("fechaHoy") LocalDate fechaHoy);
}
