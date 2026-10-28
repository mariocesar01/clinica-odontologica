package com.clinica.odontologica.dto;

import com.clinica.odontologica.enums.StatusEscala;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

public class EscalaDiaFormDTO {

    @NotNull
    private Long id;

    @NotNull(message = "Informe a hora de início.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "Informe a hora de fim.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaFim;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime pausaInicio;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime pausaFim;

    @NotNull(message = "Informe o status da escala.")
    private StatusEscala status;

    private String observacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public LocalTime getPausaInicio() { return pausaInicio; }
    public void setPausaInicio(LocalTime pausaInicio) { this.pausaInicio = pausaInicio; }
    public LocalTime getPausaFim() { return pausaFim; }
    public void setPausaFim(LocalTime pausaFim) { this.pausaFim = pausaFim; }
    public StatusEscala getStatus() { return status; }
    public void setStatus(StatusEscala status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
