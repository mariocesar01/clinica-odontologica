package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.EscalaDentista;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.PagamentoRepository;
import com.clinica.odontologica.repository.ProntuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultaService {

    private final ConsultaRepository repository;
    private final PagamentoRepository pagamentoRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final EscalaDentistaService escalaService;
    private final CobrancaService cobrancaService;

    public ConsultaService(ConsultaRepository repository,
                           PagamentoRepository pagamentoRepository,
                           ProntuarioRepository prontuarioRepository,
                           EscalaDentistaService escalaService, CobrancaService cobrancaService) {
        this.repository = repository;
        this.pagamentoRepository = pagamentoRepository;
        this.prontuarioRepository = prontuarioRepository;
        this.escalaService = escalaService;
        this.cobrancaService = cobrancaService;
    }

    public List<Consulta> listarTodos() { return repository.findAll(); }

    public List<Consulta> listarPorDentista(Long dentistaId) {
        return repository.findByDentistaIdOrderByDataHoraDesc(dentistaId);
    }

    public List<Consulta> listarAtendimentosRealizados(Long dentistaId) {
        return repository.findByDentistaIdAndStatusOrderByDataHoraDesc(dentistaId, StatusConsulta.REALIZADA);
    }

    public List<Consulta> listarAtendimentosRealizadosPorPeriodo(Long dentistaId,
                                                                  LocalDate dataInicio,
                                                                  LocalDate dataFim) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.plusDays(1).atStartOfDay().minusNanos(1);
        return repository.findByDentistaIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(
                dentistaId, StatusConsulta.REALIZADA, inicio, fim);
    }

    public Consulta buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada."));
    }

    @Transactional
    public Consulta salvar(Consulta consulta) {
        if (consulta.getStatus() == null) {
            consulta.setStatus(StatusConsulta.AGENDADA);
        }

        if (consulta.getProcedimento() == null) {
            throw new IllegalArgumentException("O procedimento é obrigatório.");
        }

        if (consulta.getId() == null && consulta.getValorProcedimento() == null) {
            consulta.setValorProcedimento(consulta.getProcedimento().getValor());
        }
        if (consulta.getId() != null && pagamentoRepository.findByConsultaId(consulta.getId()).isPresent()) {
            Consulta anterior = repository.findById(consulta.getId()).orElseThrow();
            if (!anterior.getPaciente().getId().equals(consulta.getPaciente().getId()) ||
                !anterior.getProcedimento().getId().equals(consulta.getProcedimento().getId()))
                throw new IllegalArgumentException("Consulta com cobrança não permite trocar paciente ou procedimento.");
        }
        if (consulta.getId() == null || consulta.getDuracaoMinutos() == null) {
            consulta.setDuracaoMinutos(consulta.getProcedimento().getDuracaoMinutos());
        }
        normalizarDataHora(consulta);
        validarDataHora(consulta);

        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            throw new IllegalArgumentException("Cancele a consulta pela ação Cancelar, para atualizar também a cobrança.");
        }
        if (consulta.getStatus() != StatusConsulta.CANCELADA) {
            validarEscala(consulta);
            validarConflitos(consulta);
        }

        Consulta salva = repository.save(consulta);
        cobrancaService.garantirCobranca(salva);
        return salva;
    }

    public void excluir(Long id) {
        Consulta consulta = buscarPorId(id);
        if (consulta.getStatus() == StatusConsulta.REALIZADA ||
            consulta.getStatus() == StatusConsulta.EM_ATENDIMENTO ||
            consulta.getStatus() == StatusConsulta.AGUARDANDO) {
            throw new IllegalArgumentException("Consulta em andamento ou realizada não pode ser excluída.");
        }
        if (pagamentoRepository.findByConsultaId(id).isPresent()) {
            throw new IllegalArgumentException("A consulta possui cobrança registrada. Use Cancelar para preservar o histórico financeiro.");
        }
        if (prontuarioRepository.findByConsultaId(id).isPresent()) {
            throw new IllegalArgumentException("Não é possível excluir uma consulta que possui prontuário vinculado.");
        }
        repository.deleteById(id);
    }

    public List<Consulta> listarPorStatus(StatusConsulta status) {
        return repository.findByStatusOrderByDataHoraAsc(status);
    }

    @Transactional
    public Consulta cancelar(Long id) {
        Consulta consulta = buscarPorId(id);
        if (consulta.getStatus() == StatusConsulta.REALIZADA ||
            consulta.getStatus() == StatusConsulta.EM_ATENDIMENTO ||
            consulta.getStatus() == StatusConsulta.AGUARDANDO) {
            throw new IllegalArgumentException("Consulta em andamento ou realizada não pode ser cancelada.");
        }
        if (pagamentoRepository.findByConsultaId(id)
                .filter(p -> p.getStatus() == com.clinica.odontologica.enums.StatusPagamento.PAGO).isPresent()) {
            throw new IllegalArgumentException("Consulta já paga: realize o estorno pelo provedor antes de cancelar.");
        }
        cobrancaService.cancelarCobrancaPendente(id);
        consulta.setStatus(StatusConsulta.CANCELADA);
        return repository.save(consulta);
    }

    private void normalizarDataHora(Consulta consulta) {
        if (consulta.getDataHora() != null) {
            consulta.setDataHora(consulta.getDataHora().withSecond(0).withNano(0));
        }
    }

    private void validarDataHora(Consulta consulta) {
        if (consulta.getDataHora() == null) {
            throw new IllegalArgumentException("A data e hora da consulta são obrigatórias.");
        }

        if (consulta.getDataHora().isBefore(LocalDateTime.now())
                && (consulta.getStatus() == StatusConsulta.AGENDADA
                || consulta.getStatus() == StatusConsulta.CONFIRMADA)) {
            throw new IllegalArgumentException("Uma consulta agendada ou confirmada não pode ficar no passado.");
        }

        int minuto = consulta.getDataHora().getMinute();
        if (minuto != 0 && minuto != 30) {
            throw new IllegalArgumentException("Os horários devem começar em intervalos de 30 minutos.");
        }
    }

    private void validarEscala(Consulta consulta) {
        LocalDate data = consulta.getDataHora().toLocalDate();
        EscalaDentista escala = escalaService.buscarEscalaAtiva(consulta.getDentista().getId(), data)
                .orElseThrow(() -> new IllegalArgumentException("O dentista não está escalado para esta data."));

        LocalDateTime inicio = consulta.getDataHora();
        LocalDateTime fim = consulta.getDataHoraFim();
        LocalDateTime inicioExpediente = data.atTime(escala.getHoraInicio());
        LocalDateTime fimExpediente = data.atTime(escala.getHoraFim());

        if (inicio.isBefore(inicioExpediente) || fim.isAfter(fimExpediente)) {
            throw new IllegalArgumentException("A consulta precisa ficar totalmente dentro do horário de trabalho do dentista.");
        }

        if (escala.getPausaInicio() != null && escala.getPausaFim() != null) {
            LocalDateTime pausaInicio = data.atTime(escala.getPausaInicio());
            LocalDateTime pausaFim = data.atTime(escala.getPausaFim());
            if (intervalosSobrepoem(inicio, fim, pausaInicio, pausaFim)) {
                throw new IllegalArgumentException("A consulta invade o horário de pausa do dentista.");
            }
        }
    }

    private void validarConflitos(Consulta consulta) {
        LocalDate data = consulta.getDataHora().toLocalDate();
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.plusDays(1).atStartOfDay();
        LocalDateTime inicio = consulta.getDataHora();
        LocalDateTime fim = consulta.getDataHoraFim();

        List<Consulta> consultasDentista = repository.findByDentistaIdAndDataHoraBetweenOrderByDataHoraAsc(
                consulta.getDentista().getId(), inicioDia, fimDia);

        boolean dentistaOcupado = consultasDentista.stream()
                .filter(c -> c.getStatus() != StatusConsulta.CANCELADA)
                .filter(c -> consulta.getId() == null || !c.getId().equals(consulta.getId()))
                .anyMatch(c -> intervalosSobrepoem(inicio, fim, c.getDataHora(), c.getDataHoraFim()));

        if (dentistaOcupado) {
            throw new IllegalArgumentException("Este período já está ocupado para o dentista selecionado.");
        }

        List<Consulta> consultasPaciente = repository.findByPacienteIdAndDataHoraBetweenOrderByDataHoraAsc(
                consulta.getPaciente().getId(), inicioDia, fimDia);

        boolean pacienteOcupado = consultasPaciente.stream()
                .filter(c -> c.getStatus() != StatusConsulta.CANCELADA)
                .filter(c -> consulta.getId() == null || !c.getId().equals(consulta.getId()))
                .anyMatch(c -> intervalosSobrepoem(inicio, fim, c.getDataHora(), c.getDataHoraFim()));

        if (pacienteOcupado) {
            throw new IllegalArgumentException("O paciente já possui outra consulta nesse período.");
        }
    }

    private boolean intervalosSobrepoem(LocalDateTime inicioA, LocalDateTime fimA,
                                        LocalDateTime inicioB, LocalDateTime fimB) {
        return inicioA.isBefore(fimB) && fimA.isAfter(inicioB);
    }
}
