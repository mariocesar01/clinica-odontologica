package com.clinica.odontologica.controller;

import com.clinica.odontologica.dto.EscalaDiaFormDTO;
import com.clinica.odontologica.dto.EscalaMensalFormDTO;
import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.enums.StatusEscala;
import com.clinica.odontologica.model.EscalaDentista;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.service.EscalaDentistaService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/escalas")
public class EscalaDentistaController {

    private final EscalaDentistaService service;
    private final DentistaRepository dentistaRepository;
    private final SessaoService sessaoService;

    public EscalaDentistaController(EscalaDentistaService service,
                                    DentistaRepository dentistaRepository,
                                    SessaoService sessaoService) {
        this.service = service;
        this.dentistaRepository = dentistaRepository;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(@RequestParam(required = false) Integer ano,
                               @RequestParam(required = false) Integer mes,
                               @RequestParam(required = false) String mensagem,
                               HttpSession session) {
        if (!permitido(session)) return negar();

        LocalDate hoje = LocalDate.now();
        int anoSelecionado = ano == null ? hoje.getYear() : ano;
        int mesSelecionado = mes == null ? hoje.getMonthValue() : mes;

        ModelAndView mv = new ModelAndView("escalas/lista");
        mv.addObject("escalas", service.listarMes(anoSelecionado, mesSelecionado));
        mv.addObject("ano", anoSelecionado);
        mv.addObject("mes", mesSelecionado);
        mv.addObject("mensagem", mensagem);
        return mv;
    }

    @GetMapping("/gerar")
    public ModelAndView gerarForm(HttpSession session) {
        if (!permitido(session)) return negar();
        LocalDate hoje = LocalDate.now();
        EscalaMensalFormDTO form = new EscalaMensalFormDTO();
        form.setAno(hoje.getYear());
        form.setMes(hoje.getMonthValue());
        form.setHoraInicio(LocalTime.of(8, 0));
        form.setHoraFim(LocalTime.of(18, 0));
        return formularioGeracao(form, null);
    }

    @PostMapping("/gerar")
    public ModelAndView gerar(@Valid @ModelAttribute("escalaMensal") EscalaMensalFormDTO form,
                              BindingResult result,
                              HttpSession session) {
        if (!permitido(session)) return negar();
        if (result.hasErrors()) return formularioGeracao(form, null);

        try {
            int criadas = service.gerarEscalaMensal(form);
            String mensagem = criadas + " dia(s) de escala criado(s). Registros já existentes foram preservados.";
            return new ModelAndView("redirect:/escalas?ano=" + form.getAno()
                    + "&mes=" + form.getMes() + "&mensagem=" + java.net.URLEncoder.encode(mensagem, java.nio.charset.StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return formularioGeracao(form, e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        EscalaDentista escala = service.buscarPorId(id);
        EscalaDiaFormDTO form = new EscalaDiaFormDTO();
        form.setId(escala.getId());
        form.setHoraInicio(escala.getHoraInicio());
        form.setHoraFim(escala.getHoraFim());
        form.setPausaInicio(escala.getPausaInicio());
        form.setPausaFim(escala.getPausaFim());
        form.setStatus(escala.getStatus());
        form.setObservacao(escala.getObservacao());
        return formularioEdicao(form, escala, null);
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid @ModelAttribute("escalaDia") EscalaDiaFormDTO form,
                               BindingResult result,
                               HttpSession session) {
        if (!permitido(session)) return negar();
        EscalaDentista escala = service.buscarPorId(form.getId());
        if (result.hasErrors()) return formularioEdicao(form, escala, null);

        try {
            service.atualizarDia(form);
            return new ModelAndView("redirect:/escalas?ano=" + escala.getData().getYear()
                    + "&mes=" + escala.getData().getMonthValue());
        } catch (IllegalArgumentException e) {
            return formularioEdicao(form, escala, e.getMessage());
        }
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        EscalaDentista escala = service.buscarPorId(id);
        int ano = escala.getData().getYear();
        int mes = escala.getData().getMonthValue();
        service.excluir(id);
        return new ModelAndView("redirect:/escalas?ano=" + ano + "&mes=" + mes);
    }

    private ModelAndView formularioGeracao(EscalaMensalFormDTO form, String erro) {
        ModelAndView mv = new ModelAndView("escalas/gerar");
        mv.addObject("escalaMensal", form);
        mv.addObject("dentistas", dentistaRepository.findAll());
        mv.addObject("mensagemErro", erro);
        return mv;
    }

    private ModelAndView formularioEdicao(EscalaDiaFormDTO form, EscalaDentista escala, String erro) {
        ModelAndView mv = new ModelAndView("escalas/editar");
        mv.addObject("escalaDia", form);
        mv.addObject("escala", escala);
        mv.addObject("statusEscala", StatusEscala.values());
        mv.addObject("mensagemErro", erro);
        return mv;
    }

    private boolean permitido(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.ADMINISTRADOR);
    }

    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
