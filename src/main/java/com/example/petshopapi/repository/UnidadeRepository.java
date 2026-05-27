package com.example.petshopapi.repository;

import com.example.petshopapi.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
    List<Unidade> findByNomeContainingIgnoreCase(String nome);
}
