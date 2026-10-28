package com.clinica.odontologica.repository;

import com.clinica.odontologica.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByCpf(String cpf);
    Optional<Paciente> findByUsuarioId(Long usuarioId);
    boolean existsByCpf(String cpf);
    List<Paciente> findByUsuarioNomeContainingIgnoreCase(String nome);
}
