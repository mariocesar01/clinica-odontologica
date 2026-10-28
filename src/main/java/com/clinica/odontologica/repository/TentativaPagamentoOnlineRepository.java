package com.clinica.odontologica.repository;
import com.clinica.odontologica.model.TentativaPagamentoOnline;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TentativaPagamentoOnlineRepository extends JpaRepository<TentativaPagamentoOnline, Long> {
    Optional<TentativaPagamentoOnline> findByReferenciaExterna(String referencia);
    Optional<TentativaPagamentoOnline> findByIdPagamentoProvedor(String providerId);
    List<TentativaPagamentoOnline> findByPagamentoIdOrderByCriadaEmDesc(Long pagamentoId);
    boolean existsByPagamentoId(Long pagamentoId);
}
