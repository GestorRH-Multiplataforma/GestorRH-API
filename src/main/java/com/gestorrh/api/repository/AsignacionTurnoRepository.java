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
     * @param idEmpresa El ID de la empresa.
     * @param departamento El nombre del departamento (ignora mayúsculas/minúsculas).
     * @return Lista de asignaciones para ese departamento.
     */
    List<AsignacionTurno> findByEmpleadoEmpresaIdEmpresaAndEmpleadoDepartamentoIgnoreCase(Long idEmpresa, String departamento);

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
     * @param idEmpleado El ID del empleado.
     * @param fechaInicio Fecha inicial del rango.
     * @param fechaFin Fecha final del rango.
     * @return Lista de asignaciones en el periodo indicado.
     */
    List<AsignacionTurno> findByEmpleadoIdEmpleadoAndFechaBetween(Long idEmpleado, LocalDate fechaInicio, LocalDate fechaFin);

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
