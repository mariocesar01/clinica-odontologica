package com.clinica.odontologica.repository;

import com.clinica.odontologica.enums.StatusEscala;
import com.clinica.odontologica.model.EscalaDentista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EscalaDentistaRepository extends JpaRepository<EscalaDentista, Long> {
    Optional<EscalaDentista> findByDentistaIdAndData(Long dentistaId, LocalDate data);
    List<EscalaDentista> findByDataAndStatus(LocalDate data, StatusEscala status);
    List<EscalaDentista> findByDataBetween(LocalDate inicio, LocalDate fim);
    boolean existsByDentistaId(Long dentistaId);
}
