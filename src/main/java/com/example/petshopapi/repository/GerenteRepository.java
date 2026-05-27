package com.example.petshopapi.repository;

import com.example.petshopapi.model.Gerente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GerenteRepository extends JpaRepository<Gerente, Long> {
    List<Gerente> findByNomeContainingIgnoreCase(String nome);
}
