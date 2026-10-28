package com.clinica.odontologica.controller;

import com.clinica.odontologica.dto.ProntuarioFormDTO;
import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.Prontuario;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.service.ProntuarioService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;

@Controller
@RequestMapping("/prontuarios")
public class ProntuarioController {

    private final ProntuarioService service;
    private final ConsultaRepository consultaRepository;
    private final DentistaRepository dentistaRepository;
    private final SessaoService sessaoService;

    public ProntuarioController(ProntuarioService service,
                                ConsultaRepository consultaRepository,
                                DentistaRepository dentistaRepository,
                                SessaoService sessaoService) {
        this.service = service;
        this.consultaRepository = consultaRepository;
        this.dentistaRepository = dentistaRepository;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session) {
        if (!permitido(session)) return negar();
        return new ModelAndView("redirect:/atendimentos");
    }

    @GetMapping("/novo")
    public ModelAndView novo(HttpSession session) {
        if (!permitido(session)) return negar();
        return new ModelAndView("redirect:/agenda");
    }

    @GetMapping("/atender/{consultaId}")
    public ModelAndView atender(@PathVariable Long consultaId, HttpSession session) {
        if (!permitido(session)) return negar();

        Dentista dentista = dentistaLogado(session);
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta não encontrada."));

        if (!consulta.getDentista().getId().equals(dentista.getId())) return negar();
        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            return redirecionarAgendaComErro(consulta, "Não é possível atender uma consulta cancelada.");
        }
        if (consulta.getDataHora().toLocalDate().isAfter(LocalDate.now())) {
            return redirecionarAgendaComErro(consulta, "Não é possível finalizar um atendimento em uma data futura.");
        }

        return service.buscarPorConsulta(consultaId)
                .<ModelAndView>map(prontuario -> new ModelAndView("redirect:/prontuarios/ver/" + prontuario.getId()))
                .orElseGet(() -> {
                    ProntuarioFormDTO form = new ProntuarioFormDTO();
                    form.setConsultaId(consultaId);
                    return formulario(form, consulta, null);
                });
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid @ModelAttribute("prontuarioForm") ProntuarioFormDTO form,
                               BindingResult result,
                               HttpSession session) {
        if (!permitido(session)) return negar();

        Dentista dentista = dentistaLogado(session);
        Consulta consulta = form.getConsultaId() == null
                ? null
                : consultaRepository.findById(form.getConsultaId()).orElse(null);

        if (result.hasErrors()) return formulario(form, consulta, null);
        if (consulta == null) {
            return formulario(form, null, "Consulta não encontrada.");
        }
        if (!consulta.getDentista().getId().equals(dentista.getId())) return negar();

        try {
            if (consulta.getStatus() == StatusConsulta.CANCELADA) {
                throw new IllegalArgumentException("Não é possível registrar prontuário de uma consulta cancelada.");
            }

            Prontuario prontuario = form.getId() == null ? new Prontuario() : service.buscarPorId(form.getId());
            if (prontuario.getId() != null) {
                if (!prontuario.getConsulta().getDentista().getId().equals(dentista.getId())) {
                    return negar();
                }
                if (!prontuario.getConsulta().getId().equals(consulta.getId())) {
                    throw new IllegalArgumentException("A consulta vinculada ao prontuário não pode ser alterada.");
                }
            }

            prontuario.setConsulta(consulta);
            prontuario.setDiagnostico(form.getDiagnostico());
            prontuario.setTratamento(form.getTratamento());
            prontuario.setObservacoesClinicas(form.getObservacoesClinicas());
            Prontuario salvo = service.salvar(prontuario);
            return new ModelAndView("redirect:/prontuarios/ver/" + salvo.getId());
        } catch (IllegalArgumentException e) {
            return formulario(form, consulta, e.getMessage());
        }
    }

    @GetMapping("/ver/{id}")
    public ModelAndView visualizar(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();

        Prontuario prontuario = service.buscarPorId(id);
        Dentista dentista = dentistaLogado(session);

        ModelAndView mv = new ModelAndView("prontuarios/detalhes");
        mv.addObject("prontuario", prontuario);
        mv.addObject("podeEditar", prontuario.getConsulta().getDentista().getId().equals(dentista.getId()));
        return mv;
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();

        Dentista dentista = dentistaLogado(session);
        Prontuario prontuario = service.buscarPorId(id);
        if (!prontuario.getConsulta().getDentista().getId().equals(dentista.getId())) return negar();

        ProntuarioFormDTO form = new ProntuarioFormDTO();
        form.setId(prontuario.getId());
        form.setConsultaId(prontuario.getConsulta().getId());
        form.setDiagnostico(prontuario.getDiagnostico());
        form.setTratamento(prontuario.getTratamento());
        form.setObservacoesClinicas(prontuario.getObservacoesClinicas());
        return formulario(form, prontuario.getConsulta(), null);
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!permitido(session)) return negar();

        Dentista dentista = dentistaLogado(session);
        Prontuario prontuario = service.buscarPorId(id);
        if (!prontuario.getConsulta().getDentista().getId().equals(dentista.getId())) return negar();

        service.excluir(id);
        return new ModelAndView("redirect:/atendimentos");
    }

    private ModelAndView formulario(ProntuarioFormDTO form,
                                    Consulta consulta,
                                    String mensagemErro) {
        ModelAndView mv = new ModelAndView("prontuarios/formulario");
        mv.addObject("prontuarioForm", form);
        mv.addObject("consulta", consulta);
        mv.addObject("dataConsulta", consulta == null ? null : consulta.getDataHora().toLocalDate());
        mv.addObject("mensagemErro", mensagemErro);
        return mv;
    }

    private ModelAndView redirecionarAgendaComErro(Consulta consulta, String mensagem) {
        String erro = java.net.URLEncoder.encode(mensagem, java.nio.charset.StandardCharsets.UTF_8);
        return new ModelAndView("redirect:/agenda?data=" + consulta.getDataHora().toLocalDate() + "&erro=" + erro);
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
