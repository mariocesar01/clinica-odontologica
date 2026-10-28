package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.service.RelatorioService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Controller
public class RelatorioTelaController {
    private final RelatorioService relatorios;
    private final SessaoService sessao;

    public RelatorioTelaController(RelatorioService relatorios, SessaoService sessao) {
        this.relatorios = relatorios;
        this.sessao = sessao;
    }

    @GetMapping("/admin/relatorios")
    public ModelAndView mostrar(@RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            HttpSession session) {
        if (!sessao.possuiPerfil(session, Perfil.ADMINISTRADOR)) {
            return new ModelAndView("redirect:/acesso-negado");
        }
        LocalDate hoje = LocalDate.now();
        LocalDate de = inicio == null ? hoje.withDayOfMonth(1) : inicio;
        LocalDate ate = fim == null ? hoje : fim;
        ModelAndView mv = new ModelAndView("relatorios/index");
        mv.addObject("inicio", de);
        mv.addObject("fim", ate);
        try {
            var relatorio = relatorios.gerar(de, ate);
            mv.addObject("relatorio", relatorio);
            mv.addObject("maxProc", Math.max(1L, relatorio.procedimentosRealizados().stream()
                    .mapToLong(p -> p.quantidade()).max().orElse(0)));
            mv.addObject("maxDent", Math.max(1L, relatorio.atendimentosPorDentista().stream()
                    .mapToLong(p -> p.quantidade()).max().orElse(0)));
        } catch (ResponseStatusException erro) {
            mv.addObject("erro", erro.getReason());
        }
        return mv;
    }
}
