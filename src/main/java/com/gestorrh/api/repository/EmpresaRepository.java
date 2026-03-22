package com.gestorrh.api.repository;

import com.gestorrh.api.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar las operaciones de base de datos de la entidad {@link Empresa}.
 * Proporciona métodos para la persistencia y consulta de los datos de las empresas registradas.
 */
@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /**
     * Busca una empresa por su correo electrónico institucional.
     * Método clave para el proceso de autenticación de administradores de empresa.
     * @param email El correo electrónico a buscar.
     * @return Un Optional que contiene la Empresa si se encuentra, o vacío en caso contrario.
     */
    Optional<Empresa> findByEmail(String email);

}
