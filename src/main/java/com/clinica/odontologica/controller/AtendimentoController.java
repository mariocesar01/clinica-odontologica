package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.Prontuario;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.repository.ProntuarioRepository;
import com.clinica.odontologica.service.ConsultaService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/atendimentos")
public class AtendimentoController {

    private final ConsultaService consultaService;
    private final DentistaRepository dentistaRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final SessaoService sessaoService;

    public AtendimentoController(ConsultaService consultaService,
                                 DentistaRepository dentistaRepository,
                                 ProntuarioRepository prontuarioRepository,
                                 SessaoService sessaoService) {
        this.consultaService = consultaService;
        this.dentistaRepository = dentistaRepository;
        this.prontuarioRepository = prontuarioRepository;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            HttpSession session) {

        if (!sessaoService.possuiPerfil(session, Perfil.DENTISTA)) {
            return new ModelAndView("redirect:/acesso-negado");
        }

        Usuario usuario = sessaoService.usuarioLogado(session);
        Dentista dentista = dentistaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário dentista sem cadastro profissional vinculado."));

        ModelAndView mv = new ModelAndView("atendimentos/lista");
        mv.addObject("dataInicio", dataInicio);
        mv.addObject("dataFim", dataFim);

        List<Consulta> atendimentos;
        if ((dataInicio == null) != (dataFim == null)) {
            mv.addObject("mensagemErro", "Informe a data inicial e a data final para aplicar o filtro.");
            atendimentos = consultaService.listarAtendimentosRealizados(dentista.getId());
        } else if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            mv.addObject("mensagemErro", "A data inicial não pode ser posterior à data final.");
            atendimentos = consultaService.listarAtendimentosRealizados(dentista.getId());
        } else if (dataInicio != null) {
            atendimentos = consultaService.listarAtendimentosRealizadosPorPeriodo(
                    dentista.getId(), dataInicio, dataFim);
        } else {
            atendimentos = consultaService.listarAtendimentosRealizados(dentista.getId());
        }

        Map<Long, Prontuario> prontuarioPorConsulta = new LinkedHashMap<>();
        for (Consulta consulta : atendimentos) {
            prontuarioRepository.findByConsultaId(consulta.getId())
                    .ifPresent(prontuario -> prontuarioPorConsulta.put(consulta.getId(), prontuario));
        }

        mv.addObject("atendimentos", atendimentos);
        mv.addObject("prontuarioPorConsulta", prontuarioPorConsulta);
        return mv;
    }
}
