package com.clinica.odontologica.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens", indexes = @Index(name = "idx_mensagens_conversa_data", columnList = "conversa_id,enviada_em"))
public class Mensagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;
    @ManyToOne(optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;
    @Column(nullable = false, length = 2000)
    private String texto;
    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm = LocalDateTime.now();
    @Column(name = "lida_em")
    private LocalDateTime lidaEm;
    public Long getId() { return id; }
    public Conversa getConversa() { return conversa; }
    public void setConversa(Conversa conversa) { this.conversa = conversa; }
    public Usuario getRemetente() { return remetente; }
    public void setRemetente(Usuario remetente) { this.remetente = remetente; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDateTime getEnviadaEm() { return enviadaEm; }
    public LocalDateTime getLidaEm() { return lidaEm; }
    public void setLidaEm(LocalDateTime lidaEm) { this.lidaEm = lidaEm; }
}
