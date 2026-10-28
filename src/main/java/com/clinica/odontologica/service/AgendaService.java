package com.clinica.odontologica.service;

import com.clinica.odontologica.dto.HorarioAgendaDTO;
import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.EscalaDentista;
import com.clinica.odontologica.model.Procedimento;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.ProcedimentoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgendaService {

    private static final int INTERVALO_MINUTOS = 30;

    private final ConsultaRepository consultaRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final EscalaDentistaService escalaService;

    public AgendaService(ConsultaRepository consultaRepository,
                         ProcedimentoRepository procedimentoRepository,
                         EscalaDentistaService escalaService) {
        this.consultaRepository = consultaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.escalaService = escalaService;
    }

    public List<HorarioAgendaDTO> montarAgenda(Long dentistaId, LocalDate data, Long procedimentoId) {
        EscalaDentista escala = escalaService.buscarEscalaAtiva(dentistaId, data)
                .orElseThrow(() -> new IllegalArgumentException("O dentista não possui escala ativa nesta data."));

        Procedimento procedimento = procedimentoRepository.findById(procedimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Procedimento não encontrado."));

        if (!Boolean.TRUE.equals(procedimento.getAtivo())) {
            throw new IllegalArgumentException("O procedimento selecionado está inativo.");
        }

        int duracao = procedimento.getDuracaoMinutos();
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.plusDays(1).atStartOfDay();
        List<Consulta> consultas = consultaRepository
                .findByDentistaIdAndDataHoraBetweenOrderByDataHoraAsc(dentistaId, inicioDia, fimDia);

        List<HorarioAgendaDTO> horarios = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioExpediente = data.atTime(escala.getHoraInicio());
        LocalDateTime fimExpediente = data.atTime(escala.getHoraFim());

        for (LocalDateTime inicio = inicioExpediente;
             !inicio.plusMinutes(duracao).isAfter(fimExpediente);
             inicio = inicio.plusMinutes(INTERVALO_MINUTOS)) {

            LocalDateTime horarioInicio = inicio;
            LocalDateTime horarioFim = horarioInicio.plusMinutes(duracao);

            if (horarioInicio.isBefore(agora)) {
                horarios.add(new HorarioAgendaDTO(horarioInicio, horarioFim, false, "Horário passado", null));
                continue;
            }

            if (sobrepoePausa(horarioInicio, horarioFim, escala)) {
                horarios.add(new HorarioAgendaDTO(horarioInicio, horarioFim, false, "Pausa do dentista", null));
                continue;
            }

            Consulta conflito = consultas.stream()
                    .filter(c -> c.getStatus() != StatusConsulta.CANCELADA)
                    .filter(c -> intervalosSobrepoem(horarioInicio, horarioFim, c.getDataHora(), c.getDataHoraFim()))
                    .findFirst()
                    .orElse(null);

            if (conflito != null) {
                horarios.add(new HorarioAgendaDTO(horarioInicio, horarioFim, false, "Ocupado", conflito));
            } else {
                horarios.add(new HorarioAgendaDTO(horarioInicio, horarioFim, true, "Disponível", null));
            }
        }

        return horarios;
    }

    private boolean sobrepoePausa(LocalDateTime inicio,
                                  LocalDateTime fim,
                                  EscalaDentista escala) {
        if (escala.getPausaInicio() == null || escala.getPausaFim() == null) return false;
        LocalDate data = inicio.toLocalDate();
        LocalDateTime pausaInicio = data.atTime(escala.getPausaInicio());
        LocalDateTime pausaFim = data.atTime(escala.getPausaFim());
        return intervalosSobrepoem(inicio, fim, pausaInicio, pausaFim);
    }

    private boolean intervalosSobrepoem(LocalDateTime inicioA, LocalDateTime fimA,
                                        LocalDateTime inicioB, LocalDateTime fimB) {
        return inicioA.isBefore(fimB) && fimA.isAfter(inicioB);
    }
}
