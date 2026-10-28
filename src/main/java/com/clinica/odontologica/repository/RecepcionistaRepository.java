package com.clinica.odontologica.repository;

import com.clinica.odontologica.model.Recepcionista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecepcionistaRepository extends JpaRepository<Recepcionista, Long> {
    Optional<Recepcionista> findByCpf(String cpf);
    List<Recepcionista> findByUsuarioNomeContainingIgnoreCase(String nome);
}
