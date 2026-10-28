package com.clinica.odontologica.model;

import com.clinica.odontologica.enums.StatusConsulta;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "consultas")
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O paciente é obrigatório.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @NotNull(message = "O dentista é obrigatório.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "dentista_id", nullable = false)
    private Dentista dentista;

    @ManyToOne
    @JoinColumn(name = "procedimento_id")
    private Procedimento procedimento;

    @NotNull(message = "A data e hora são obrigatórias.")
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    @Column(name = "valor_procedimento", precision = 10, scale = 2)
    private BigDecimal valorProcedimento;

    @Column(name = "inicio_atendimento")
    private LocalDateTime inicioAtendimento;

    @Column(name = "fim_atendimento")
    private LocalDateTime fimAtendimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConsulta status = StatusConsulta.AGENDADA;

    @Column(length = 500)
    private String observacoes;

    @Transient
    public int getDuracaoEfetiva() {
        if (duracaoMinutos != null && duracaoMinutos > 0) return duracaoMinutos;
        if (procedimento != null) return procedimento.getDuracaoMinutos();
        return 30;
    }

    @Transient
    public LocalDateTime getDataHoraFim() {
        return dataHora == null ? null : dataHora.plusMinutes(getDuracaoEfetiva());
    }

    public BigDecimal getValorProcedimento() { return valorProcedimento; }
    public void setValorProcedimento(BigDecimal valorProcedimento) { this.valorProcedimento = valorProcedimento; }
    public LocalDateTime getInicioAtendimento() { return inicioAtendimento; }
    public void setInicioAtendimento(LocalDateTime inicioAtendimento) { this.inicioAtendimento = inicioAtendimento; }
    public LocalDateTime getFimAtendimento() { return fimAtendimento; }
    public void setFimAtendimento(LocalDateTime fimAtendimento) { this.fimAtendimento = fimAtendimento; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Dentista getDentista() { return dentista; }
    public void setDentista(Dentista dentista) { this.dentista = dentista; }
    public Procedimento getProcedimento() { return procedimento; }
    public void setProcedimento(Procedimento procedimento) { this.procedimento = procedimento; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public StatusConsulta getStatus() { return status; }
    public void setStatus(StatusConsulta status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
