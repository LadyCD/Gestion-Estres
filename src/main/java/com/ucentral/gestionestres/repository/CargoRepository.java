package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Integer> {

    List<Cargo> findAllByOrderByNombreAsc();
}
