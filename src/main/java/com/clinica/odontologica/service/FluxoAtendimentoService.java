package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.*;
import com.clinica.odontologica.model.*;
import com.clinica.odontologica.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class FluxoAtendimentoService {
    private final ConsultaRepository consultas;
    private final DentistaRepository dentistas;
    public FluxoAtendimentoService(ConsultaRepository consultas, DentistaRepository dentistas) {
        this.consultas = consultas; this.dentistas = dentistas;
    }
    @Transactional
    public Consulta chegada(Long id, Usuario usuario) {
        if (usuario == null || usuario.getPerfil() != Perfil.RECEPCIONISTA)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente a recepção pode registrar a chegada.");
        Consulta c = localizar(id);
        if (!c.getDataHora().toLocalDate().equals(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chegada permitida apenas na data da consulta.");
        if (c.getStatus() != StatusConsulta.AGENDADA && c.getStatus() != StatusConsulta.CONFIRMADA)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Consulta não está aguardando chegada.");
        c.setStatus(StatusConsulta.AGUARDANDO);
        return consultas.save(c);
    }
    @Transactional
    public Consulta iniciar(Long id, Usuario usuario) {
        if (usuario == null || usuario.getPerfil() != Perfil.DENTISTA)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente dentista pode iniciar o atendimento.");
        Dentista dentista = dentistas.findByUsuarioId(usuario.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Dentista não cadastrado."));
        Consulta c = localizar(id);
        if (!c.getDentista().getId().equals(dentista.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Consulta de outro dentista.");
        if (!c.getDataHora().toLocalDate().equals(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Atendimento só pode iniciar na data da consulta.");
        if (c.getStatus() != StatusConsulta.AGUARDANDO && c.getStatus() != StatusConsulta.CONFIRMADA
                && c.getStatus() != StatusConsulta.AGENDADA)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Consulta não pode ser iniciada nesse estado.");
        c.setInicioAtendimento(LocalDateTime.now()); c.setStatus(StatusConsulta.EM_ATENDIMENTO);
        return consultas.save(c);
    }
    private Consulta localizar(Long id) {
        return consultas.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta não encontrada."));
    }
    public List<Consulta> consultarDia(LocalDate data, Usuario usuario) {
        if (usuario == null || usuario.getPerfil() != Perfil.RECEPCIONISTA)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente recepção.");
        return consultas.findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
                data.atStartOfDay(), data.plusDays(1).atStartOfDay());
    }
    public int percentual(StatusConsulta status) {
        return switch(status) {
            case AGENDADA -> 0;
            case CONFIRMADA -> 20;
            case AGUARDANDO -> 40;
            case EM_ATENDIMENTO -> 70;
            case REALIZADA -> 100;
            case CANCELADA -> 0;
        };
    }
    public record Acompanhamento(Long id, String hora, String paciente, String dentista,
                                  String procedimento, String status, int progresso) {}
    public List<Acompanhamento> dados(LocalDate data, Usuario usuario) {
        return consultarDia(data, usuario).stream().map(c -> new Acompanhamento(c.getId(),
                c.getDataHora().toLocalTime().toString(), c.getPaciente().getUsuario().getNome(),
                c.getDentista().getUsuario().getNome(), c.getProcedimento() == null ? "" : c.getProcedimento().getNome(),
                c.getStatus().name(), percentual(c.getStatus()))).toList();
    }
    public int progressoGlobal(List<Acompanhamento> itens) {
        List<Acompanhamento> ativos = itens.stream().filter(a -> !a.status().equals("CANCELADA")).toList();
        return ativos.isEmpty() ? 0 : (int) Math.round(ativos.stream().mapToInt(Acompanhamento::progresso).average().orElse(0));
    }
}
