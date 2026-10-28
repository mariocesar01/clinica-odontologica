package com.clinica.odontologica.repository;

import com.clinica.odontologica.model.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {
    List<Procedimento> findByAtivoTrueOrderByNomeAsc();
    List<Procedimento> findByNomeContainingIgnoreCase(String nome);
}
