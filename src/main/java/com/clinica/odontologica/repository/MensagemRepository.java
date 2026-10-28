package com.clinica.odontologica.repository;
import com.clinica.odontologica.model.Mensagem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    List<Mensagem> findByConversaIdOrderByEnviadaEmDesc(Long conversaId, Pageable pageable);
    List<Mensagem> findByConversaIdAndLidaEmIsNullAndRemetenteIdNot(Long conversaId, Long usuarioId);
}
