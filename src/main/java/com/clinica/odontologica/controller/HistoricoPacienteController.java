package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.Paciente;
import com.clinica.odontologica.model.Prontuario;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.service.PacienteService;
import com.clinica.odontologica.service.ProntuarioService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/historico-pacientes")
public class HistoricoPacienteController {

    private final PacienteService pacienteService;
    private final ProntuarioService prontuarioService;
    private final DentistaRepository dentistaRepository;
    private final SessaoService sessaoService;

    public HistoricoPacienteController(PacienteService pacienteService,
                                       ProntuarioService prontuarioService,
                                       DentistaRepository dentistaRepository,
                                       SessaoService sessaoService) {
        this.pacienteService = pacienteService;
        this.prontuarioService = prontuarioService;
        this.dentistaRepository = dentistaRepository;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(@RequestParam(defaultValue = "") String nome,
                               HttpSession session) {
        if (!permitido(session)) return negar();

        List<Paciente> pacientes = nome.isBlank()
                ? pacienteService.listarTodos()
                : pacienteService.pesquisarPorNome(nome);

        ModelAndView mv = new ModelAndView("historico-pacientes/lista");
        mv.addObject("pacientes", pacientes);
        mv.addObject("nomePesquisa", nome);
        return mv;
    }

    @GetMapping("/{id}")
    public ModelAndView visualizar(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();

        Paciente paciente = pacienteService.buscarPorId(id);
        List<Prontuario> prontuarios = prontuarioService.listarPorPaciente(id);
        Dentista dentistaLogado = dentistaLogado(session);

        ModelAndView mv = new ModelAndView("historico-pacientes/detalhes");
        mv.addObject("paciente", paciente);
        mv.addObject("prontuarios", prontuarios);
        mv.addObject("dentistaLogadoId", dentistaLogado.getId());
        return mv;
    }

    private Dentista dentistaLogado(HttpSession session) {
        Usuario usuario = sessaoService.usuarioLogado(session);
        return dentistaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário dentista sem cadastro profissional vinculado."));
    }

    private boolean permitido(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.DENTISTA);
    }

    private ModelAndView negar() {
        return new ModelAndView("redirect:/acesso-negado");
    }
}
