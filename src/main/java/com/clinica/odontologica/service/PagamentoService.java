package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.FormaPagamento;
import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.enums.StatusPagamento;
import com.clinica.odontologica.model.Pagamento;
import com.clinica.odontologica.repository.PagamentoRepository;
import com.clinica.odontologica.repository.TentativaPagamentoOnlineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PagamentoService {
    private final PagamentoRepository pagamentos;
    private final TentativaPagamentoOnlineRepository tentativas;

    public PagamentoService(PagamentoRepository pagamentos, TentativaPagamentoOnlineRepository tentativas) {
        this.pagamentos = pagamentos;
        this.tentativas = tentativas;
    }

    public List<Pagamento> listarTodos() { return pagamentos.findAll(); }
    public boolean temTentativaOnline(Long id) { return tentativas.existsByPagamentoId(id); }

    public Pagamento buscarPorId(Long id) {
        return pagamentos.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Cobrança não encontrada."));
    }

    /** Somente confirmação presencial; cobrança e valor são definidos pelo agendamento. */
    @Transactional
    public Pagamento registrarPresencial(Long id, FormaPagamento forma) {
        if (forma == null) throw new IllegalArgumentException("Selecione a forma de pagamento.");
        Pagamento pagamento = buscarPorId(id);
        if (pagamento.getStatus() != StatusPagamento.PENDENTE)
            throw new IllegalArgumentException("A cobrança não está pendente.");
        if (pagamento.getConsulta().getStatus() == StatusConsulta.CANCELADA)
            throw new IllegalArgumentException("A consulta está cancelada.");
        if (tentativas.existsByPagamentoId(id))
            throw new IllegalArgumentException("A cobrança possui tentativas online. Verifique-as antes de registrar pagamento presencial.");
        pagamento.setFormaPagamento(forma);
        pagamento.setDataPagamento(LocalDate.now());
        pagamento.setStatus(StatusPagamento.PAGO);
        return pagamentos.save(pagamento);
    }
}
