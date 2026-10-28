package com.clinica.odontologica.model;

import com.clinica.odontologica.enums.StatusTentativaPagamento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tentativas_pagamento_online")
public class TentativaPagamentoOnline {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "pagamento_id", nullable = false)
    private Pagamento pagamento;
    @Column(name = "referencia_externa", nullable = false, unique = true, length = 120)
    private String referenciaExterna;
    @Column(name = "preferencia_id", length = 150)
    private String preferenciaId;
    @Column(name = "url_checkout", length = 1200)
    private String urlCheckout;
    @Column(name = "id_pagamento_provedor", unique = true, length = 120)
    private String idPagamentoProvedor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTentativaPagamento status = StatusTentativaPagamento.CRIADA;
    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();
    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;
    public Long getId() { return id; }
    public Pagamento getPagamento() { return pagamento; }
    public void setPagamento(Pagamento pagamento) { this.pagamento = pagamento; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) { this.referenciaExterna = referenciaExterna; }
    public String getPreferenciaId() { return preferenciaId; }
    public void setPreferenciaId(String preferenciaId) { this.preferenciaId = preferenciaId; }
    public String getUrlCheckout() { return urlCheckout; }
    public void setUrlCheckout(String urlCheckout) { this.urlCheckout = urlCheckout; }
    public String getIdPagamentoProvedor() { return idPagamentoProvedor; }
    public void setIdPagamentoProvedor(String idPagamentoProvedor) { this.idPagamentoProvedor = idPagamentoProvedor; }
    public StatusTentativaPagamento getStatus() { return status; }
    public void setStatus(StatusTentativaPagamento status) { this.status = status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(LocalDateTime atualizadaEm) { this.atualizadaEm = atualizadaEm; }
}
