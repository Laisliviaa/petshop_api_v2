package com.example.petshopapi.repository;

import com.example.petshopapi.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findByDescricaoContainingIgnoreCase(String descricao);
}
