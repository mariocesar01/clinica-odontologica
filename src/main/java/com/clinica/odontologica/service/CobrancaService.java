package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.enums.StatusPagamento;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.Pagamento;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.PagamentoRepository;
import com.clinica.odontologica.repository.TentativaPagamentoOnlineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Uma consulta tem no máximo uma cobrança, criada sem ação manual da recepção. */
@Service
public class CobrancaService {
    private final PagamentoRepository pagamentos;
    private final ConsultaRepository consultas;
    private final TentativaPagamentoOnlineRepository tentativas;

    public CobrancaService(PagamentoRepository pagamentos, ConsultaRepository consultas,
                          TentativaPagamentoOnlineRepository tentativas) {
        this.pagamentos = pagamentos;
        this.consultas = consultas;
        this.tentativas = tentativas;
    }

    @Transactional
    public void garantirCobranca(Consulta consulta) {
        if (consulta.getId() == null || consulta.getStatus() == StatusConsulta.CANCELADA ||
                consulta.getProcedimento() == null) return;
        BigDecimal valor = consulta.getValorProcedimento() == null
                ? consulta.getProcedimento().getValor() : consulta.getValorProcedimento();
        if (valor == null || valor.signum() <= 0) return; // Procedimento gratuito não gera cobrança.
        if (pagamentos.findByConsultaId(consulta.getId()).isPresent()) return;
        Pagamento pagamento = new Pagamento();
        pagamento.setConsulta(consulta);
        pagamento.setValor(valor);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamentos.save(pagamento);
    }

    /** Não deixa uma cobrança pendente continuar pagável após cancelamento. */
    @Transactional
    public void cancelarCobrancaPendente(Long consultaId) {
        pagamentos.findByConsultaId(consultaId).ifPresent(p -> {
            if (p.getStatus() == StatusPagamento.PAGO)
                throw new IllegalArgumentException("Consulta paga: solicite o estorno antes de cancelar.");
            if (tentativas.existsByPagamentoId(p.getId()))
                throw new IllegalArgumentException("Existe uma tentativa de pagamento online. Regularize-a com a recepção antes de cancelar.");
            if (p.getStatus() == StatusPagamento.PENDENTE) {
                p.setStatus(StatusPagamento.CANCELADO);
                pagamentos.save(p);
            }
        });
    }

    /** Regulariza consultas anteriores à implantação das cobranças automáticas. */
    @Transactional
    public void gerarCobrancasAusentes() {
        for (Consulta consulta : consultas.findAll()) garantirCobranca(consulta);
    }
}
