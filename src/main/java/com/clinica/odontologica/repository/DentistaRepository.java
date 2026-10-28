package com.clinica.odontologica.repository;

import com.clinica.odontologica.model.Dentista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DentistaRepository extends JpaRepository<Dentista, Long> {
    Optional<Dentista> findByCroIgnoreCase(String cro);
    boolean existsByCroIgnoreCase(String cro);
    List<Dentista> findByUsuarioNomeContainingIgnoreCase(String nome);
    Optional<Dentista> findByUsuarioId(Long usuarioId);
}
