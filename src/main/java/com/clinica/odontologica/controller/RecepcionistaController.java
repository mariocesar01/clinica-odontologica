package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Recepcionista;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.service.RecepcionistaService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/recepcionistas")
public class RecepcionistaController {

    private final RecepcionistaService service;
    private final SessaoService sessaoService;

    public RecepcionistaController(RecepcionistaService service, SessaoService sessaoService) {
        this.service = service;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session) {
        if (!administrador(session)) return negar();
        ModelAndView mv = new ModelAndView("recepcionistas/lista");
        mv.addObject("recepcionistas", service.listarTodos());
        return mv;
    }

    @GetMapping("/novo")
    public ModelAndView novo(HttpSession session) {
        if (!administrador(session)) return negar();
        Recepcionista recepcionista = new Recepcionista();
        recepcionista.setUsuario(new Usuario());
        return formulario(recepcionista, null);
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid Recepcionista recepcionista,
                               BindingResult result,
                               HttpSession session) {
        if (!administrador(session)) return negar();
        if (result.hasErrors()) return formulario(recepcionista, null);

        try {
            service.salvar(recepcionista);
            return new ModelAndView("redirect:/recepcionistas");
        } catch (IllegalArgumentException e) {
            return formulario(recepcionista, e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!administrador(session)) return negar();
        Recepcionista recepcionista = service.buscarPorId(id);
        recepcionista.getUsuario().setSenha("");
        return formulario(recepcionista, null);
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!administrador(session)) return negar();
        service.excluir(id);
        return new ModelAndView("redirect:/recepcionistas");
    }

    @GetMapping("/pesquisar")
    public ModelAndView pesquisar(@RequestParam(defaultValue = "") String nome, HttpSession session) {
        if (!administrador(session)) return negar();
        ModelAndView mv = new ModelAndView("recepcionistas/lista");
        mv.addObject("recepcionistas", nome.isBlank() ? service.listarTodos() : service.pesquisarPorNome(nome));
        mv.addObject("nomePesquisa", nome);
        return mv;
    }

    private ModelAndView formulario(Recepcionista recepcionista, String mensagemErro) {
        ModelAndView mv = new ModelAndView("recepcionistas/formulario");
        mv.addObject("recepcionista", recepcionista);
        mv.addObject("mensagemErro", mensagemErro);
        return mv;
    }

    private boolean administrador(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.ADMINISTRADOR);
    }

    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
