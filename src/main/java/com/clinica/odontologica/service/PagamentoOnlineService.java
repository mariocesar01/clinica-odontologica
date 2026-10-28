package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.*;
import com.clinica.odontologica.model.*;
import com.clinica.odontologica.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PagamentoOnlineService {
    private final PagamentoRepository pagamentos;
    private final TentativaPagamentoOnlineRepository tentativas;
    private final ConsultaRepository consultas;
    private final PacienteRepository pacientes;
    private final MercadoPagoClient mp;
    public PagamentoOnlineService(PagamentoRepository pagamentos, TentativaPagamentoOnlineRepository tentativas,
                                  ConsultaRepository consultas, PacienteRepository pacientes, MercadoPagoClient mp) {
        this.pagamentos = pagamentos; this.tentativas = tentativas; this.consultas = consultas;
        this.pacientes = pacientes; this.mp = mp;
    }
    public record Checkout(Long tentativaId, Long pagamentoId, String status, String url) {}

    public Paciente meuPaciente(Usuario usuario) {
        if (usuario == null || usuario.getPerfil() != Perfil.PACIENTE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente pacientes.");
        return pacientes.findByUsuarioId(usuario.getId()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.FORBIDDEN, "Paciente sem cadastro vinculado."));
    }
    public Consulta consultaMinha(Long consultaId, Usuario usuario) {
        Paciente p = meuPaciente(usuario);
        Consulta c = consultas.findById(consultaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta não encontrada."));
        if (!c.getPaciente().getId().equals(p.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Consulta de outro paciente.");
        return c;
    }
    public List<Consulta> minhasConsultas(Usuario usuario) {
        Paciente p = meuPaciente(usuario);
        return consultas.findByPacienteIdOrderByDataHoraDesc(p.getId());
    }
    public Map<String,Object> statusDaConsulta(Long consultaId, Usuario usuario) {
        Consulta consulta = consultaMinha(consultaId, usuario);
        return pagamentos.findByConsultaId(consultaId).<Map<String,Object>>map(p -> Map.of(
                "consultaId", consulta.getId(), "pagamentoId", p.getId(),
                "valor", p.getValor(), "status", p.getStatus().name(),
                "tentativas", tentativas.findByPagamentoIdOrderByCriadaEmDesc(p.getId()).size()))
                .orElseGet(() -> Map.of("consultaId", consulta.getId(), "status", "NAO_GERADO"));
    }
    @Transactional
    public Checkout iniciar(Long consultaId, Usuario usuario) {
        Consulta consulta = consultaMinha(consultaId, usuario);
        if (consulta.getStatus() == StatusConsulta.CANCELADA)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Consulta cancelada.");
        if (consulta.getProcedimento() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Consulta sem procedimento definido.");
        BigDecimal valor = consulta.getValorProcedimento() != null ? consulta.getValorProcedimento()
                : consulta.getProcedimento().getValor();
        if (valor == null || valor.signum() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Valor do procedimento inválido.");
        Pagamento pagamento = pagamentos.findByConsultaId(consultaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT,
                        "Esta consulta ainda não possui cobrança automática. Consulte a recepção."));
        if (pagamento.getStatus() == StatusPagamento.PAGO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pagamento já realizado.");
        // Não misturar tentativa online com valor escolhido manualmente pela recepção.
        if (pagamento.getValor().compareTo(valor) != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Valor cadastrado diverge do procedimento. Consulte a recepção.");
        if (pagamento.getStatus() == StatusPagamento.CANCELADO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cobrança cancelada.");
        if (!mp.configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Configure MP_ACCESS_TOKEN para pagamentos online.");
        // Evita gerar diversos checkouts quando o paciente clica várias vezes em poucos minutos.
        Optional<TentativaPagamentoOnline> recente = tentativas.findByPagamentoIdOrderByCriadaEmDesc(pagamento.getId())
                .stream().filter(t -> t.getStatus() == StatusTentativaPagamento.PENDENTE
                        && t.getUrlCheckout() != null
                        && t.getCriadaEm().isAfter(LocalDateTime.now().minusMinutes(30))).findFirst();
        if (recente.isPresent()) {
            TentativaPagamentoOnline anterior = recente.get();
            return new Checkout(anterior.getId(), pagamento.getId(), anterior.getStatus().name(), anterior.getUrlCheckout());
        }
        TentativaPagamentoOnline t = new TentativaPagamentoOnline();
        t.setPagamento(pagamento);
        t.setReferenciaExterna("CLINICA-" + pagamento.getId() + "-" + UUID.randomUUID());
        t = tentativas.save(t);
        MercadoPagoClient.Preferencia pref = mp.criarPreferencia(t.getReferenciaExterna(),
                consulta.getProcedimento().getNome() + " - consulta " + consulta.getId(), valor);
        t.setPreferenciaId(pref.id()); t.setUrlCheckout(pref.url()); t.setStatus(StatusTentativaPagamento.PENDENTE);
        t.setAtualizadaEm(LocalDateTime.now()); tentativas.save(t);
        return new Checkout(t.getId(), pagamento.getId(), t.getStatus().name(), pref.url());
    }
    @Transactional(readOnly = true)
    public List<TentativaPagamentoOnline> tentativasMinhas(Long consultaId, Usuario usuario) {
        consultaMinha(consultaId, usuario);
        return pagamentos.findByConsultaId(consultaId)
                .map(p -> tentativas.findByPagamentoIdOrderByCriadaEmDesc(p.getId())).orElseGet(List::of);
    }

    @Transactional
    public void atualizarPorPagamentoProvedor(String idPagamento) {
        JsonNode resposta = mp.consultarPagamento(idPagamento);
        String referencia = resposta.path("external_reference").asText("");
        // Uma transação alheia à clínica não pode alterar nosso banco.
        Optional<TentativaPagamentoOnline> encontrada = tentativas.findByReferenciaExterna(referencia);
        if (encontrada.isEmpty()) return;
        TentativaPagamentoOnline tentativa = encontrada.get();
        Pagamento pagamento = tentativa.getPagamento();
        if (!idPagamento.equals(resposta.path("id").asText()) ||
                !"BRL".equals(resposta.path("currency_id").asText()) ||
                resposta.path("transaction_amount").decimalValue().compareTo(pagamento.getValor()) != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dados de pagamento divergentes.");
        if (tentativa.getIdPagamentoProvedor() != null && !idPagamento.equals(tentativa.getIdPagamentoProvedor()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tentativa vinculada a outro pagamento.");
        String status = resposta.path("status").asText("");
        // Transição terminal não é revertida por notificações antigas (idempotência).
        if (tentativa.getStatus() == StatusTentativaPagamento.APROVADA && !"refunded".equals(status) && !"charged_back".equals(status)) return;
        tentativa.setIdPagamentoProvedor(idPagamento);
        tentativa.setAtualizadaEm(LocalDateTime.now());
        switch (status) {
            case "approved" -> {
                tentativa.setStatus(StatusTentativaPagamento.APROVADA);
                pagamento.setStatus(StatusPagamento.PAGO);
                String tipo = resposta.path("payment_type_id").asText("");
                pagamento.setFormaPagamento(switch(tipo) {
                    case "credit_card" -> FormaPagamento.CREDITO;
                    case "debit_card" -> FormaPagamento.DEBITO;
                    default -> FormaPagamento.PIX;
                });
                pagamento.setDataPagamento(LocalDate.now());
                pagamentos.save(pagamento);
            }
            case "rejected" -> tentativa.setStatus(StatusTentativaPagamento.RECUSADA);
            case "cancelled", "refunded", "charged_back" -> {
                boolean estaTentativaHaviaSidoAprovada = tentativa.getStatus() == StatusTentativaPagamento.APROVADA;
                tentativa.setStatus(StatusTentativaPagamento.CANCELADA);
                if (pagamento.getStatus() == StatusPagamento.PAGO &&
                    estaTentativaHaviaSidoAprovada && !"cancelled".equals(status)) {
                    pagamento.setStatus(StatusPagamento.CANCELADO);
                    pagamentos.save(pagamento);
                }
            }
            default -> tentativa.setStatus(StatusTentativaPagamento.PENDENTE);
        }
        tentativas.save(tentativa);
    }
    @Transactional
    public void verificarTentativa(Long tentativaId, Usuario usuario) {
        TentativaPagamentoOnline tentativa = tentativas.findById(tentativaId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        consultaMinha(tentativa.getPagamento().getConsulta().getId(), usuario);
        if (tentativa.getIdPagamentoProvedor() != null)
            atualizarPorPagamentoProvedor(tentativa.getIdPagamentoProvedor());
        else {
            // Verificação manual segura para demonstração local, mesmo sem webhook público.
            for (String pagamentoId : mp.pesquisarIdsPorReferencia(tentativa.getReferenciaExterna())) {
                atualizarPorPagamentoProvedor(pagamentoId); // ID, referência, moeda e valor conferidos pelo serviço.
            }
        }
        // O navegador nunca informa o resultado do pagamento; consultamos o Mercado Pago.
    }
}
