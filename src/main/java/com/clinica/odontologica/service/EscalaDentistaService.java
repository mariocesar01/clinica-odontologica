package com.clinica.odontologica.service;

import com.clinica.odontologica.dto.EscalaDiaFormDTO;
import com.clinica.odontologica.dto.EscalaMensalFormDTO;
import com.clinica.odontologica.enums.StatusEscala;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.EscalaDentista;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.repository.EscalaDentistaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class EscalaDentistaService {

    private final EscalaDentistaRepository repository;
    private final DentistaRepository dentistaRepository;

    public EscalaDentistaService(EscalaDentistaRepository repository,
                                 DentistaRepository dentistaRepository) {
        this.repository = repository;
        this.dentistaRepository = dentistaRepository;
    }

    public List<EscalaDentista> listarMes(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        List<EscalaDentista> escalas = repository.findByDataBetween(
                periodo.atDay(1), periodo.atEndOfMonth());
        escalas.sort(Comparator.comparing(EscalaDentista::getData)
                .thenComparing(e -> e.getDentista().getUsuario().getNome()));
        return escalas;
    }

    public EscalaDentista buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada."));
    }

    public Optional<EscalaDentista> buscarEscalaAtiva(Long dentistaId, LocalDate data) {
        return repository.findByDentistaIdAndData(dentistaId, data)
                .filter(e -> e.getStatus() == StatusEscala.ATIVO);
    }

    public List<EscalaDentista> listarAtivasPorData(LocalDate data) {
        List<EscalaDentista> escalas = repository.findByDataAndStatus(data, StatusEscala.ATIVO);
        escalas.sort(Comparator.comparing(e -> e.getDentista().getUsuario().getNome()));
        return escalas;
    }

    @Transactional
    public int gerarEscalaMensal(EscalaMensalFormDTO form) {
        validarPeriodo(form);
        validarHorarios(form.getHoraInicio(), form.getHoraFim(), form.getPausaInicio(), form.getPausaFim());

        if (!form.possuiDiaSelecionado()) {
            throw new IllegalArgumentException("Selecione pelo menos um dia da semana.");
        }

        Dentista dentista = dentistaRepository.findById(form.getDentistaId())
                .orElseThrow(() -> new IllegalArgumentException("Dentista não encontrado."));

        YearMonth periodo = YearMonth.of(form.getAno(), form.getMes());
        int criadas = 0;

        for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
            LocalDate data = periodo.atDay(dia);
            if (!form.trabalhaNoDia(data.getDayOfWeek())) continue;

            if (repository.findByDentistaIdAndData(dentista.getId(), data).isPresent()) {
                continue;
            }

            EscalaDentista escala = new EscalaDentista();
            escala.setDentista(dentista);
            escala.setData(data);
            escala.setHoraInicio(form.getHoraInicio());
            escala.setHoraFim(form.getHoraFim());
            escala.setPausaInicio(form.getPausaInicio());
            escala.setPausaFim(form.getPausaFim());
            escala.setStatus(StatusEscala.ATIVO);
            repository.save(escala);
            criadas++;
        }

        return criadas;
    }

    @Transactional
    public EscalaDentista atualizarDia(EscalaDiaFormDTO form) {
        EscalaDentista escala = buscarPorId(form.getId());
        validarHorarios(form.getHoraInicio(), form.getHoraFim(), form.getPausaInicio(), form.getPausaFim());

        escala.setHoraInicio(form.getHoraInicio());
        escala.setHoraFim(form.getHoraFim());
        escala.setPausaInicio(form.getPausaInicio());
        escala.setPausaFim(form.getPausaFim());
        escala.setStatus(form.getStatus());
        escala.setObservacao(form.getObservacao());
        return repository.save(escala);
    }

    public void excluir(Long id) {
        repository.deleteById(id);
    }

    private void validarPeriodo(EscalaMensalFormDTO form) {
        if (form.getMes() < 1 || form.getMes() > 12) {
            throw new IllegalArgumentException("Mês inválido.");
        }
        if (form.getAno() < 2020) {
            throw new IllegalArgumentException("Ano inválido.");
        }
    }

    private void validarHorarios(java.time.LocalTime inicio,
                                 java.time.LocalTime fim,
                                 java.time.LocalTime pausaInicio,
                                 java.time.LocalTime pausaFim) {
        if (inicio == null || fim == null || !inicio.isBefore(fim)) {
            throw new IllegalArgumentException("A hora de início deve ser anterior à hora de fim.");
        }

        boolean apenasUmaPausa = (pausaInicio == null) != (pausaFim == null);
        if (apenasUmaPausa) {
            throw new IllegalArgumentException("Informe o início e o fim da pausa, ou deixe os dois campos vazios.");
        }

        validarGrade30(inicio);
        validarGrade30(fim);
        if (pausaInicio != null) {
            validarGrade30(pausaInicio);
            validarGrade30(pausaFim);
            if (!pausaInicio.isBefore(pausaFim)) {
                throw new IllegalArgumentException("O início da pausa deve ser anterior ao fim da pausa.");
            }
            if (pausaInicio.isBefore(inicio) || pausaFim.isAfter(fim)) {
                throw new IllegalArgumentException("A pausa precisa estar dentro do horário de trabalho.");
            }
        }
    }

    private void validarGrade30(java.time.LocalTime horario) {
        if (horario.getMinute() != 0 && horario.getMinute() != 30) {
            throw new IllegalArgumentException("Os horários da escala devem usar intervalos de 30 minutos.");
        }
    }
}
