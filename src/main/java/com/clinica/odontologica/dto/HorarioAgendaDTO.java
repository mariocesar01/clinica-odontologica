package com.clinica.odontologica.dto;

import com.clinica.odontologica.model.Consulta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HorarioAgendaDTO {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalDateTime inicio;
    private final LocalDateTime fim;
    private final boolean disponivel;
    private final String motivo;
    private final Consulta consulta;

    public HorarioAgendaDTO(LocalDateTime inicio,
                            LocalDateTime fim,
                            boolean disponivel,
                            String motivo,
                            Consulta consulta) {
        this.inicio = inicio;
        this.fim = fim;
        this.disponivel = disponivel;
        this.motivo = motivo;
        this.consulta = consulta;
    }

    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
    public boolean isDisponivel() { return disponivel; }
    public String getMotivo() { return motivo; }
    public Consulta getConsulta() { return consulta; }
    public String getHoraInicio() { return inicio.format(HORA_FORMATTER); }
    public String getHoraFim() { return fim.format(HORA_FORMATTER); }
    public String getPeriodo() { return getHoraInicio() + " - " + getHoraFim(); }
}
