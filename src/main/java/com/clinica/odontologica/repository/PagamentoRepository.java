package com.clinica.odontologica.repository;

import com.clinica.odontologica.enums.StatusPagamento;
import com.clinica.odontologica.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Optional<Pagamento> findByConsultaId(Long consultaId);
    List<Pagamento> findByStatus(StatusPagamento status);
    long countByStatus(StatusPagamento status);
}
