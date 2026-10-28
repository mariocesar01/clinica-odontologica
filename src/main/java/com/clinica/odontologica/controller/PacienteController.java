package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Paciente;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.service.PacienteService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final SessaoService sessaoService;

    public PacienteController(PacienteService pacienteService, SessaoService sessaoService) {
        this.pacienteService = pacienteService;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session,
                               @RequestParam(required = false) String mensagem,
                               @RequestParam(required = false) String erro) {
        if (!permitido(session)) return negar();
        ModelAndView mv = new ModelAndView("pacientes/lista");
        mv.addObject("pacientes", pacienteService.listarTodos());
        mv.addObject("mensagem", mensagem);
        mv.addObject("mensagemErro", erro);
        return mv;
    }

    @GetMapping("/novo")
    public ModelAndView novo(HttpSession session) {
        if (!permitido(session)) return negar();
        Paciente paciente = new Paciente();
        paciente.setUsuario(new Usuario());
        return formulario(paciente, null);
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid Paciente paciente, BindingResult result, HttpSession session) {
        if (!permitido(session)) return negar();
        if (result.hasErrors()) return formulario(paciente, null);

        try {
            pacienteService.salvar(paciente);
            return new ModelAndView("redirect:/pacientes");
        } catch (IllegalArgumentException e) {
            return formulario(paciente, e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        Paciente paciente = pacienteService.buscarPorId(id);
        paciente.getUsuario().setSenha("");
        return formulario(paciente, null);
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();
        try {
            pacienteService.excluir(id);
            return new ModelAndView("redirect:/pacientes");
        } catch (IllegalArgumentException e) {
            ModelAndView mv = new ModelAndView("pacientes/lista");
            mv.addObject("pacientes", pacienteService.listarTodos());
            mv.addObject("mensagemErro", e.getMessage());
            return mv;
        }
    }

    @GetMapping("/pesquisar")
    public ModelAndView pesquisar(@RequestParam(defaultValue = "") String nome, HttpSession session) {
        if (!permitido(session)) return negar();
        ModelAndView mv = new ModelAndView("pacientes/lista");
        mv.addObject("pacientes", nome.isBlank() ? pacienteService.listarTodos() : pacienteService.pesquisarPorNome(nome));
        mv.addObject("nomePesquisa", nome);
        return mv;
    }

    private ModelAndView formulario(Paciente paciente, String mensagemErro) {
        ModelAndView mv = new ModelAndView("pacientes/formulario");
        mv.addObject("paciente", paciente);
        mv.addObject("mensagemErro", mensagemErro);
        return mv;
    }

    private boolean permitido(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.ADMINISTRADOR, Perfil.RECEPCIONISTA);
    }

    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
