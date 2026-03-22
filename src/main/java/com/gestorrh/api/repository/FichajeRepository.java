package com.gestorrh.api.repository;

import com.gestorrh.api.entity.Fichaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FichajeRepository extends JpaRepository<Fichaje, Long> {

    List<Fichaje> findByEmpleadoIdEmpleadoAndFechaAndHoraSalidaIsNull(Long idEmpleado, LocalDate fecha);

    List<Fichaje> findByEmpleadoIdEmpleadoAndFechaBetween(Long idEmpleado, LocalDate fechaInicio, LocalDate fechaFin);

    List<Fichaje> findByEmpleadoEmpresaIdEmpresaAndFechaBetween(Long idEmpresa, LocalDate fechaInicio, LocalDate fechaFin);

    @Query("SELECT f.empleado.nombre, COUNT(f) " +
            "FROM Fichaje f " +
            "WHERE f.empleado.empresa.idEmpresa = :idEmpresa " +
            "AND f.incidencias LIKE '%fuera de horario%' " +
            "GROUP BY f.empleado.nombre " +
            "ORDER BY COUNT(f) DESC")
    List<Object[]> obtenerTopRetrasos(@Param("idEmpresa") Long idEmpresa);
}
