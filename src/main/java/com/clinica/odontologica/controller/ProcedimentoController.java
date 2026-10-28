package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Procedimento;
import com.clinica.odontologica.service.ProcedimentoService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/procedimentos")
public class ProcedimentoController {

    private final ProcedimentoService service;
    private final SessaoService sessaoService;

    public ProcedimentoController(ProcedimentoService service, SessaoService sessaoService) {
        this.service = service;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session,
                               @RequestParam(required = false) String mensagem,
                               @RequestParam(required = false) String erro) {
        if (!permitido(session)) return negar();
        ModelAndView mv = new ModelAndView("procedimentos/lista");
        mv.addObject("procedimentos", service.listarTodos());
        mv.addObject("mensagem", mensagem);
        mv.addObject("mensagemErro", erro);
        return mv;
    }

    @GetMapping("/novo")
    public ModelAndView novo(HttpSession session) {
        if (!permitido(session)) return negar();
        return formulario(new Procedimento(), null);
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid Procedimento procedimento, BindingResult result, HttpSession session) {
        if (!permitido(session)) return negar();
        if (result.hasErrors()) return formulario(procedimento, null);
        try {
            service.salvar(procedimento);
            return new ModelAndView("redirect:/procedimentos");
        } catch (IllegalArgumentException e) {
            return formulario(procedimento, e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        return formulario(service.buscarPorId(id), null);
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        try {
            service.excluir(id);
            return new ModelAndView("redirect:/procedimentos");
        } catch (IllegalArgumentException e) {
            return new ModelAndView("redirect:/procedimentos?erro="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/pesquisar")
    public ModelAndView pesquisar(@RequestParam(defaultValue = "") String nome, HttpSession session) {
        if (!permitido(session)) return negar();
        ModelAndView mv = new ModelAndView("procedimentos/lista");
        mv.addObject("procedimentos", nome.isBlank() ? service.listarTodos() : service.pesquisar(nome));
        mv.addObject("nomePesquisa", nome);
        return mv;
    }

    private ModelAndView formulario(Procedimento procedimento, String mensagemErro) {
        ModelAndView mv = new ModelAndView("procedimentos/formulario");
        mv.addObject("procedimento", procedimento);
        mv.addObject("mensagemErro", mensagemErro);
        return mv;
    }

    private boolean permitido(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.ADMINISTRADOR);
    }

    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
