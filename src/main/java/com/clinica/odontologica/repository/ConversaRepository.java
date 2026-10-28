package com.clinica.odontologica.repository;
import com.clinica.odontologica.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ConversaRepository extends JpaRepository<Conversa, Long> {
    Optional<Conversa> findByPacienteId(Long pacienteId);
}
