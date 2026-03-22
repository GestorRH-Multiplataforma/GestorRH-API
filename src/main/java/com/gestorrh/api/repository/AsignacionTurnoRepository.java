package com.gestorrh.api.repository;

import com.gestorrh.api.entity.AsignacionTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsignacionTurnoRepository extends JpaRepository<AsignacionTurno, Long> {

    List<AsignacionTurno> findByEmpleadoIdEmpleadoAndFecha(Long idEmpleado, LocalDate fecha);

    List<AsignacionTurno> findByEmpleadoEmpresaIdEmpresaAndEmpleadoDepartamentoIgnoreCase(Long idEmpresa, String departamento);

    List<AsignacionTurno> findByEmpleadoEmpresaIdEmpresa(Long idEmpresa);

    List<AsignacionTurno> findByEmpleadoIdEmpleado(Long idEmpleado);

    List<AsignacionTurno> findByEmpleadoIdEmpleadoAndFechaBetween(Long idEmpleado, LocalDate fechaInicio, LocalDate fechaFin);

    @Query("SELECT COUNT(DISTINCT a.empleado.idEmpleado) " +
            "FROM AsignacionTurno a " +
            "WHERE a.empleado.empresa.idEmpresa = :idEmpresa " +
            "AND a.fecha = :fechaHoy")
    Long contarEmpleadosPlanificadosHoy(@Param("idEmpresa") Long idEmpresa, @Param("fechaHoy") LocalDate fechaHoy);
}
