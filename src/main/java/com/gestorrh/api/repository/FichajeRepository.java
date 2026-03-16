package com.gestorrh.api.repository;

import com.gestorrh.api.entity.Fichaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FichajeRepository extends JpaRepository<Fichaje, Long> {

    List<Fichaje> findByEmpleadoIdEmpleadoAndFechaAndHoraSalidaIsNull(Long idEmpleado, LocalDate fecha);

    List<Fichaje> findByEmpleadoIdEmpleadoAndFechaBetween(Long idEmpleado, LocalDate fechaInicio, LocalDate fechaFin);

    List<Fichaje> findByEmpleadoEmpresaIdEmpresaAndFechaBetween(Long idEmpresa, LocalDate fechaInicio, LocalDate fechaFin);
}
