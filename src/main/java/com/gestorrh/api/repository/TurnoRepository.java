package com.gestorrh.api.repository;

import com.gestorrh.api.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para gestionar las operaciones de base de datos de la entidad {@link Turno}.
 * Permite definir los diferentes horarios y turnos de trabajo que una empresa puede asignar.
 */
@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    /**
     * Recupera todos los turnos configurados por una empresa específica.
     * Fundamental para garantizar el aislamiento de datos entre empresas (Multi-Tenant).
     * @param idEmpresa El ID de la empresa.
     * @return Lista de turnos pertenecientes a la empresa.
     */
    List<Turno> findByEmpresaIdEmpresa(Long idEmpresa);
}
