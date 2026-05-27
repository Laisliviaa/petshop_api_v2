package com.example.petshopapi.repository;

import com.example.petshopapi.model.Agendamento;
import com.example.petshopapi.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByStatus(StatusAgendamento status);
}
