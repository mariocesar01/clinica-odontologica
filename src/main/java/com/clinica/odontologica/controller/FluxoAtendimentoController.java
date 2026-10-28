package com.clinica.odontologica.controller;

import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import java.time.LocalDate;
import java.util.*;

@Controller
public class FluxoAtendimentoController {
    private final FluxoAtendimentoService fluxo;
    private final SessaoService sessao;
    public FluxoAtendimentoController(FluxoAtendimentoService fluxo, SessaoService sessao) {
        this.fluxo = fluxo; this.sessao = sessao;
    }
    @PostMapping("/api/recepcao/consultas/{id}/chegada")
    @ResponseBody
    public Map<String, String> chegada(@PathVariable Long id, HttpSession session) {
        Consulta c = fluxo.chegada(id, sessao.usuarioLogado(session)); return Map.of("status", c.getStatus().name());
    }
    @PostMapping("/api/dentista/consultas/{id}/iniciar")
    @ResponseBody
    public Map<String, String> iniciar(@PathVariable Long id, HttpSession session) {
        Consulta c = fluxo.iniciar(id, sessao.usuarioLogado(session)); return Map.of("status", c.getStatus().name());
    }
    @GetMapping("/api/recepcao/andamento")
    @ResponseBody
    public Map<String, Object> dados(
       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
       HttpSession session) {
        LocalDate dia = data == null ? LocalDate.now() : data;
        List<FluxoAtendimentoService.Acompanhamento> lista = fluxo.dados(dia, sessao.usuarioLogado(session));
        return Map.of("data", dia, "atendimentos", lista, "progressoMedio", fluxo.progressoGlobal(lista));
    }
    @GetMapping("/recepcao/andamento")
    public ModelAndView tela(HttpSession session) {
        LocalDate dia = LocalDate.now();
        List<FluxoAtendimentoService.Acompanhamento> lista = fluxo.dados(dia, sessao.usuarioLogado(session));
        ModelAndView mv = new ModelAndView("recepcao/andamento");
        mv.addObject("dia", dia); mv.addObject("atendimentos", lista);
        mv.addObject("progresso", fluxo.progressoGlobal(lista)); return mv;
    }
}
