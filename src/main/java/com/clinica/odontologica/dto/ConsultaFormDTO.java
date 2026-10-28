package com.clinica.odontologica.dto;

import com.clinica.odontologica.enums.StatusConsulta;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class ConsultaFormDTO {

    private Long id;

    @NotNull(message = "O paciente é obrigatório.")
    private Long pacienteId;

    @NotNull(message = "O dentista é obrigatório.")
    private Long dentistaId;

    @NotNull(message = "O procedimento é obrigatório.")
    private Long procedimentoId;

    @NotNull(message = "A data e hora são obrigatórias.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dataHora;

    private StatusConsulta status = StatusConsulta.AGENDADA;
    private String observacoes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getDentistaId() { return dentistaId; }
    public void setDentistaId(Long dentistaId) { this.dentistaId = dentistaId; }
    public Long getProcedimentoId() { return procedimentoId; }
    public void setProcedimentoId(Long procedimentoId) { this.procedimentoId = procedimentoId; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public StatusConsulta getStatus() { return status; }
    public void setStatus(StatusConsulta status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
