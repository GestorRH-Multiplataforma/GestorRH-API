package com.gestorrh.api.repository;

import com.gestorrh.api.entity.Ausencia;
import com.gestorrh.api.entity.enums.EstadoAusencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para gestionar las operaciones de base de datos de la entidad {@link Ausencia}.
 * Gestiona solicitudes de vacaciones, bajas médicas y otros tipos de ausencias de los empleados.
 */
@Repository
public interface AusenciaRepository extends JpaRepository<Ausencia, Long> {

    /**
     * Recupera todas las ausencias de una empresa.
     * @param idEmpresa El ID de la empresa.
     * @return Lista de ausencias de la empresa.
     */
    List<Ausencia> findByEmpleadoEmpresaIdEmpresa(Long idEmpresa);

    /**
     * Recupera las ausencias de una empresa filtradas por su estado.
     * @param idEmpresa El ID de la empresa.
     * @param estado El estado de la ausencia (PENDIENTE, APROBADA, RECHAZADA).
     * @return Lista de ausencias en ese estado.
     */
    List<Ausencia> findByEmpleadoEmpresaIdEmpresaAndEstado(Long idEmpresa, EstadoAusencia estado);

    /**
     * Busca ausencias en un departamento específico de una empresa.
     * @param idEmpresa El ID de la empresa.
     * @param departamento El nombre del departamento.
     * @return Lista de ausencias en el departamento.
     */
    List<Ausencia> findByEmpleadoEmpresaIdEmpresaAndEmpleadoDepartamentoIgnoreCase(Long idEmpresa, String departamento);

    /**
     * Busca ausencias en un departamento específico y con un estado determinado.
     * @param idEmpresa El ID de la empresa.
     * @param departamento El nombre del departamento.
     * @param estado El estado de la ausencia.
     * @return Lista de ausencias filtradas.
     */
    List<Ausencia> findByEmpleadoEmpresaIdEmpresaAndEmpleadoDepartamentoIgnoreCaseAndEstado(Long idEmpresa, String departamento, EstadoAusencia estado);

    /**
     * Recupera todas las ausencias asociadas a un empleado.
     * @param idEmpleado El ID del empleado.
     * @return Lista de ausencias del empleado.
     */
    List<Ausencia> findByEmpleadoIdEmpleado(Long idEmpleado);

    /**
     * Recupera las ausencias de un empleado filtradas por estado.
     * @param idEmpleado El ID del empleado.
     * @param estado El estado de la ausencia.
     * @return Lista de ausencias del empleado en ese estado.
     */
    List<Ausencia> findByEmpleadoIdEmpleadoAndEstado(Long idEmpleado, EstadoAusencia estado);

    /**
     * Verifica si un empleado tiene una ausencia aprobada en una fecha específica.
     * @param idEmpleado El ID del empleado.
     * @param estado El estado de la ausencia (normalmente APROBADA).
     * @param fecha La fecha a comprobar.
     * @return True si existe una ausencia aprobada en esa fecha, false en caso contrario.
     */
    @Query("SELECT COUNT(a) > 0 FROM Ausencia a " +
            "WHERE a.empleado.idEmpleado = :idEmpleado " +
            "AND a.estado = :estado " +
            "AND :fecha BETWEEN a.fechaInicio AND a.fechaFin")
    boolean tieneAusenciaAprobadaEnFecha(
            @Param("idEmpleado") Long idEmpleado,
            @Param("estado") EstadoAusencia estado,
            @Param("fecha") java.time.LocalDate fecha
    );

    /**
     * Busca ausencias de un empleado que se solapen con un rango de fechas dado.
     * @param idEmpleado El ID del empleado.
     * @param estados Lista de estados a considerar (ej. PENDIENTE, APROBADA).
     * @param fechaInicio Fecha inicial del rango.
     * @param fechaFin Fecha final del rango.
     * @return Lista de ausencias que coinciden en el tiempo con el periodo indicado.
     */
    @Query("SELECT a FROM Ausencia a WHERE a.empleado.idEmpleado = :idEmpleado " +
            "AND a.estado IN :estados " +
            "AND (a.fechaInicio <= :fechaFin AND a.fechaFin >= :fechaInicio)")
    List<Ausencia> findAusenciasSolapadas(
            @org.springframework.data.repository.query.Param("idEmpleado") Long idEmpleado,
            @org.springframework.data.repository.query.Param("estados") java.util.List<EstadoAusencia> estados,
            @org.springframework.data.repository.query.Param("fechaInicio") java.time.LocalDate fechaInicio,
            @org.springframework.data.repository.query.Param("fechaFin") java.time.LocalDate fechaFin
    );

    /**
     * Obtiene el recuento de ausencias aprobadas agrupadas por tipo para una empresa.
     * @param idEmpresa El ID de la empresa.
     * @return Lista de objetos con el tipo y el conteo.
     */
    @Query("SELECT a.tipo, COUNT(a) " +
            "FROM Ausencia a " +
            "WHERE a.empleado.empresa.idEmpresa = :idEmpresa " +
            "AND a.estado = 'APROBADA' " +
            "GROUP BY a.tipo")
    List<Object[]> contarAusenciasAprobadasPorTipo(@Param("idEmpresa") Long idEmpresa);

    /**
     * Obtiene el recuento de ausencias agrupadas por estado para una empresa.
     * @param idEmpresa El ID de la empresa.
     * @return Lista de objetos con el estado y el conteo.
     */
    @Query("SELECT a.estado, COUNT(a) " +
            "FROM Ausencia a " +
            "WHERE a.empleado.empresa.idEmpresa = :idEmpresa " +
            "GROUP BY a.estado")
    List<Object[]> contarAusenciasPorEstado(@Param("idEmpresa") Long idEmpresa);

    /**
     * Cuenta el número de empleados distintos que están ausentes hoy (con ausencia aprobada).
     * @param idEmpresa El ID de la empresa.
     * @param fechaHoy La fecha actual.
     * @return Número total de empleados ausentes hoy.
     */
    @Query("SELECT COUNT(DISTINCT a.empleado.idEmpleado) " +
            "FROM Ausencia a " +
            "WHERE a.empleado.empresa.idEmpresa = :idEmpresa " +
            "AND a.estado = 'APROBADA' " +
            "AND :fechaHoy BETWEEN a.fechaInicio AND a.fechaFin")
    Long contarEmpleadosAusentesHoy(@Param("idEmpresa") Long idEmpresa, @Param("fechaHoy") LocalDate fechaHoy);
}
