package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.enums.StatusPagamento;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class PainelService {
    private final ConsultaRepository consultas;
    private final PacienteRepository pacientes;
    private final DentistaRepository dentistas;
    private final PagamentoRepository pagamentos;
    private final RecepcionistaRepository recepcionistas;

    public PainelService(ConsultaRepository consultas, PacienteRepository pacientes,
                        DentistaRepository dentistas, PagamentoRepository pagamentos,
                        RecepcionistaRepository recepcionistas) {
        this.consultas = consultas; this.pacientes = pacientes;
        this.dentistas = dentistas; this.pagamentos = pagamentos;
        this.recepcionistas = recepcionistas;
    }

    public record Resumo(long pacientes, long dentistas, long recepcionistas,
                         long consultasHoje, long agendadas, long realizadas,
                         long aguardando, long cobrancasPendentes, BigDecimal recebido,
                         List<Consulta> proximas) {}

    public Resumo obter(Usuario usuario) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fim = inicio.plusDays(1);
        List<Consulta> dia = consultas.findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(inicio, fim);
        List<Consulta> visiveis;
        if (usuario.getPerfil() == Perfil.DENTISTA) {
            Long id = dentistas.findByUsuarioId(usuario.getId()).map(d -> d.getId()).orElse(-1L);
            visiveis = dia.stream().filter(c -> c.getDentista().getId().equals(id)).toList();
        } else if (usuario.getPerfil() == Perfil.PACIENTE) {
            Long id = pacientes.findByUsuarioId(usuario.getId()).map(p -> p.getId()).orElse(-1L);
            visiveis = consultas.findByPacienteIdOrderByDataHoraDesc(id);
        } else {
            visiveis = dia;
        }
        List<Consulta> proximas = visiveis.stream()
                .filter(c -> c.getStatus() != StatusConsulta.CANCELADA && c.getStatus() != StatusConsulta.REALIZADA)
                .filter(c -> usuario.getPerfil() != Perfil.PACIENTE || !c.getDataHora().isBefore(LocalDateTime.now().minusHours(2)))
                .sorted(Comparator.comparing(Consulta::getDataHora)).limit(5).toList();
        long agendadas = visiveis.stream().filter(c -> c.getStatus() == StatusConsulta.AGENDADA || c.getStatus() == StatusConsulta.CONFIRMADA).count();
        long realizadas = visiveis.stream().filter(c -> c.getStatus() == StatusConsulta.REALIZADA).count();
        long aguardando = visiveis.stream().filter(c -> c.getStatus() == StatusConsulta.AGUARDANDO || c.getStatus() == StatusConsulta.EM_ATENDIMENTO).count();
        long pendentes = pagamentos.countByStatus(StatusPagamento.PENDENTE);
        BigDecimal recebidos = pagamentos.findAll().stream()
                .filter(p -> p.getStatus() == StatusPagamento.PAGO && LocalDate.now().equals(p.getDataPagamento()))
                .map(p -> p.getValor()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resumo(pacientes.count(), dentistas.count(), recepcionistas.count(),
                visiveis.size(), agendadas, realizadas, aguardando, pendentes, recebidos, proximas);
    }
}
