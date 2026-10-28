package com.clinica.odontologica.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class EscalaMensalFormDTO {

    @NotNull(message = "O dentista é obrigatório.")
    private Long dentistaId;

    @Min(value = 2020, message = "Ano inválido.")
    private int ano;

    @Min(value = 1, message = "Mês inválido.")
    @Max(value = 12, message = "Mês inválido.")
    private int mes;

    private boolean segunda;
    private boolean terca;
    private boolean quarta;
    private boolean quinta;
    private boolean sexta;
    private boolean sabado;

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

    public boolean trabalhaNoDia(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> segunda;
            case TUESDAY -> terca;
            case WEDNESDAY -> quarta;
            case THURSDAY -> quinta;
            case FRIDAY -> sexta;
            case SATURDAY -> sabado;
            default -> false;
        };
    }

    public boolean possuiDiaSelecionado() {
        return segunda || terca || quarta || quinta || sexta || sabado;
    }

    public Long getDentistaId() { return dentistaId; }
    public void setDentistaId(Long dentistaId) { this.dentistaId = dentistaId; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public boolean isSegunda() { return segunda; }
    public void setSegunda(boolean segunda) { this.segunda = segunda; }
    public boolean isTerca() { return terca; }
    public void setTerca(boolean terca) { this.terca = terca; }
    public boolean isQuarta() { return quarta; }
    public void setQuarta(boolean quarta) { this.quarta = quarta; }
    public boolean isQuinta() { return quinta; }
    public void setQuinta(boolean quinta) { this.quinta = quinta; }
    public boolean isSexta() { return sexta; }
    public void setSexta(boolean sexta) { this.sexta = sexta; }
    public boolean isSabado() { return sabado; }
    public void setSabado(boolean sabado) { this.sabado = sabado; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public LocalTime getPausaInicio() { return pausaInicio; }
    public void setPausaInicio(LocalTime pausaInicio) { this.pausaInicio = pausaInicio; }
    public LocalTime getPausaFim() { return pausaFim; }
    public void setPausaFim(LocalTime pausaFim) { this.pausaFim = pausaFim; }
}
