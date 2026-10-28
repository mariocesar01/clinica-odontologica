package com.clinica.odontologica.model;

import com.clinica.odontologica.enums.StatusEscala;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "escalas_dentistas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dentista_id", "data_escala"})
)
public class EscalaDentista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O dentista é obrigatório.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "dentista_id", nullable = false)
    private Dentista dentista;

    @NotNull(message = "A data da escala é obrigatória.")
    @Column(name = "data_escala", nullable = false)
    private LocalDate data;

    @NotNull(message = "A hora de início é obrigatória.")
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull(message = "A hora de fim é obrigatória.")
    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "pausa_inicio")
    private LocalTime pausaInicio;

    @Column(name = "pausa_fim")
    private LocalTime pausaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEscala status = StatusEscala.ATIVO;

    @Column(length = 300)
    private String observacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Dentista getDentista() { return dentista; }
    public void setDentista(Dentista dentista) { this.dentista = dentista; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
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
