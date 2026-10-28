package com.clinica.odontologica.repository;

import com.clinica.odontologica.model.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProntuarioRepository extends JpaRepository<Prontuario, Long> {
    Optional<Prontuario> findByConsultaId(Long consultaId);
    List<Prontuario> findByConsultaPacienteIdOrderByConsultaDataHoraDesc(Long pacienteId);
}
