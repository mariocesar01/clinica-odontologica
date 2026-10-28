package com.clinica.odontologica.dto;

import jakarta.validation.constraints.NotNull;

public class ProntuarioFormDTO {

    private Long id;

    @NotNull(message = "A consulta é obrigatória.")
    private Long consultaId;

    private String diagnostico;
    private String tratamento;
    private String observacoesClinicas;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConsultaId() { return consultaId; }
    public void setConsultaId(Long consultaId) { this.consultaId = consultaId; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public String getTratamento() { return tratamento; }
    public void setTratamento(String tratamento) { this.tratamento = tratamento; }
    public String getObservacoesClinicas() { return observacoesClinicas; }
    public void setObservacoesClinicas(String observacoesClinicas) { this.observacoesClinicas = observacoesClinicas; }
}
