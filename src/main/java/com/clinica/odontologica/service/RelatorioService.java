package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.*;
import com.clinica.odontologica.model.*;
import com.clinica.odontologica.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class RelatorioService {
    private final ConsultaRepository consultas;
    private final PagamentoRepository pagamentos;
    private final DentistaRepository dentistas;
    public RelatorioService(ConsultaRepository consultas, PagamentoRepository pagamentos,
                            DentistaRepository dentistas) {
        this.consultas = consultas; this.pagamentos = pagamentos; this.dentistas = dentistas;
    }
    public record Quantidade(String nome, long quantidade) {}
    public record Resumo(LocalDate dataInicio, LocalDate dataFim, BigDecimal faturamento,
                         BigDecimal ticketMedio, long pagamentosRecebidos, long pagamentosPendentes,
                         long consultasRealizadas, long consultasCanceladas,
                         long consultasAgendadas, long consultasEmAndamento,
                         List<Quantidade> procedimentosRealizados, List<Quantidade> atendimentosPorDentista) {}

    public Resumo gerar(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim) ||
                inicio.isBefore(LocalDate.of(2000,1,1)) || fim.isAfter(LocalDate.now().plusYears(5)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um período válido.");
        // A data de conclusão real conta para realizados; consultas antigas usam a data agendada.
        List<Consulta> periodo = consultas.findAll().stream().filter(c -> {
            LocalDate date = c.getStatus() == StatusConsulta.REALIZADA && c.getFimAtendimento() != null
                    ? c.getFimAtendimento().toLocalDate() : c.getDataHora().toLocalDate();
            return !date.isBefore(inicio) && !date.isAfter(fim);
        }).toList();
        List<Consulta> realizados = periodo.stream().filter(c -> c.getStatus() == StatusConsulta.REALIZADA).toList();
        long canceladas = periodo.stream().filter(c -> c.getStatus() == StatusConsulta.CANCELADA).count();
        long agendadas = periodo.stream().filter(c -> c.getStatus() == StatusConsulta.AGENDADA ||
                c.getStatus() == StatusConsulta.CONFIRMADA).count();
        long andamento = periodo.stream().filter(c -> c.getStatus() == StatusConsulta.AGUARDANDO ||
                c.getStatus() == StatusConsulta.EM_ATENDIMENTO).count();
        List<Pagamento> recebidos = pagamentos.findAll().stream().filter(p ->
                p.getStatus() == StatusPagamento.PAGO && p.getDataPagamento() != null &&
                !p.getDataPagamento().isBefore(inicio) && !p.getDataPagamento().isAfter(fim)).toList();
        BigDecimal faturamento = recebidos.stream().map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticketMedio = recebidos.isEmpty() ? BigDecimal.ZERO : faturamento.divide(
                BigDecimal.valueOf(recebidos.size()), 2, RoundingMode.HALF_UP);
        long pendentes = pagamentos.findAll().stream().filter(p ->
                p.getStatus() == StatusPagamento.PENDENTE &&
                !p.getConsulta().getDataHora().toLocalDate().isBefore(inicio) &&
                !p.getConsulta().getDataHora().toLocalDate().isAfter(fim)).count();
        Map<String,Long> porProcedimento = new TreeMap<>();
        for (Consulta c : realizados) {
            String nome = c.getProcedimento() == null ? "Não informado" : c.getProcedimento().getNome();
            porProcedimento.merge(nome, 1L, Long::sum);
        }
        Map<String,Long> porDentista = new TreeMap<>();
        // Todos os dentistas existentes, inclusive os que não atenderam no período.
        dentistas.findAll().forEach(d -> porDentista.put(d.getUsuario().getNome() + " (ID " + d.getId() + ")", 0L));
        for (Consulta c : realizados) {
            Dentista d = c.getDentista();
            porDentista.merge(d.getUsuario().getNome() + " (ID " + d.getId() + ")", 1L, Long::sum);
        }
        return new Resumo(inicio, fim, faturamento, ticketMedio, recebidos.size(), pendentes,
                realizados.size(), canceladas, agendadas, andamento,
                porProcedimento.entrySet().stream().map(e -> new Quantidade(e.getKey(), e.getValue())).toList(),
                porDentista.entrySet().stream().map(e -> new Quantidade(e.getKey(), e.getValue())).toList());
    }

    private String linha(String... valores) {
        return String.join(";", Arrays.stream(valores).map(v -> {
            String texto = v == null ? "" : v;
            if (!texto.isBlank() && "=+-@".indexOf(texto.charAt(0)) >= 0) texto = "'" + texto;
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }).toList()) + "\r\n";
    }
    public String exportarCsv(Resumo r) {
        StringBuilder b = new StringBuilder("\uFEFF");
        b.append(linha("Relatório Clínica Odontológica",r.dataInicio().toString(),r.dataFim().toString()));
        b.append(linha("Indicador","Valor"));
        b.append(linha("Faturamento recebido",r.faturamento().toPlainString()));
        b.append(linha("Ticket médio",r.ticketMedio().toPlainString()));
        b.append(linha("Pagamentos recebidos",Long.toString(r.pagamentosRecebidos())));
        b.append(linha("Cobranças pendentes",Long.toString(r.pagamentosPendentes())));
        b.append(linha("Consultas realizadas",Long.toString(r.consultasRealizadas())));
        b.append(linha("Consultas canceladas",Long.toString(r.consultasCanceladas())));
        b.append(linha("Consultas agendadas",Long.toString(r.consultasAgendadas())));
        b.append(linha("Consultas em andamento",Long.toString(r.consultasEmAndamento())));
        b.append(linha("Procedimento","Realizados"));
        r.procedimentosRealizados().forEach(q -> b.append(linha(q.nome(),Long.toString(q.quantidade()))));
        b.append(linha("Dentista","Atendimentos realizados"));
        r.atendimentosPorDentista().forEach(q -> b.append(linha(q.nome(),Long.toString(q.quantidade()))));
        return b.toString();
    }
}
