package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.service.DentistaService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/dentistas")
public class DentistaController {

    private final DentistaService dentistaService;
    private final SessaoService sessaoService;

    public DentistaController(DentistaService dentistaService, SessaoService sessaoService) {
        this.dentistaService = dentistaService;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session,
                               @RequestParam(required = false) String mensagem,
                               @RequestParam(required = false) String erro) {
        if (!permitido(session)) return negar();
        ModelAndView mv = new ModelAndView("dentistas/lista");
        mv.addObject("dentistas", dentistaService.listarTodos());
        mv.addObject("mensagem", mensagem);
        mv.addObject("mensagemErro", erro);
        return mv;
    }

    @GetMapping("/novo")
    public ModelAndView novo(HttpSession session) {
        if (!permitido(session)) return negar();
        Dentista dentista = new Dentista();
        dentista.setUsuario(new Usuario());
        return formulario(dentista, null);
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid Dentista dentista, BindingResult result, HttpSession session) {
        if (!permitido(session)) return negar();
        if (result.hasErrors()) return formulario(dentista, null);
        try {
            dentistaService.salvar(dentista);
            return new ModelAndView("redirect:/dentistas");
        } catch (IllegalArgumentException e) {
            return formulario(dentista, e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        Dentista dentista = dentistaService.buscarPorId(id);
        dentista.getUsuario().setSenha("");
        return formulario(dentista, null);
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        try {
            dentistaService.excluir(id);
            return new ModelAndView("redirect:/dentistas");
        } catch (IllegalArgumentException e) {
            ModelAndView mv = new ModelAndView("dentistas/lista");
            mv.addObject("dentistas", dentistaService.listarTodos());
            mv.addObject("mensagemErro", e.getMessage());
            return mv;
        }
    }

    @GetMapping("/pesquisar")
    public ModelAndView pesquisar(@RequestParam(defaultValue = "") String nome, HttpSession session) {
        if (!permitido(session)) return negar();
        ModelAndView mv = new ModelAndView("dentistas/lista");
        mv.addObject("dentistas", nome.isBlank() ? dentistaService.listarTodos() : dentistaService.pesquisarPorNome(nome));
        mv.addObject("nomePesquisa", nome);
        return mv;
    }

    private ModelAndView formulario(Dentista dentista, String mensagemErro) {
        ModelAndView mv = new ModelAndView("dentistas/formulario");
        mv.addObject("dentista", dentista);
        mv.addObject("mensagemErro", mensagemErro);
        return mv;
    }

    private boolean permitido(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.ADMINISTRADOR);
    }

    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
