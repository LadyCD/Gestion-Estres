package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    /** HU003: la clasificación horaria se ejecuta para los trabajadores. */
    java.util.List<Usuario> findByRol(com.ucentral.gestionestres.model.Rol rol);
}
