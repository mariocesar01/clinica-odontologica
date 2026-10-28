package com.clinica.odontologica.controller;

import com.clinica.odontologica.dto.ConsultaFormDTO;
import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.Paciente;
import com.clinica.odontologica.model.Procedimento;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.repository.PacienteRepository;
import com.clinica.odontologica.repository.ProcedimentoRepository;
import com.clinica.odontologica.service.ConsultaService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService service;
    private final PacienteRepository pacienteRepository;
    private final DentistaRepository dentistaRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final SessaoService sessaoService;

    public ConsultaController(ConsultaService service,
                              PacienteRepository pacienteRepository,
                              DentistaRepository dentistaRepository,
                              ProcedimentoRepository procedimentoRepository,
                              SessaoService sessaoService) {
        this.service = service;
        this.pacienteRepository = pacienteRepository;
        this.dentistaRepository = dentistaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session,
                               @RequestParam(required = false) String mensagem,
                               @RequestParam(required = false) String erro,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                               @RequestParam(required = false) StatusConsulta status) {
        if (!podeVisualizar(session)) return negar();

        Usuario usuario = sessaoService.usuarioLogado(session);
        if (usuario.getPerfil() == Perfil.DENTISTA) {
            return new ModelAndView("redirect:/atendimentos");
        }

        ModelAndView mv = new ModelAndView("consultas/lista");
        List<Consulta> consultas = service.listarTodos();
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            mv.addObject("mensagemErro", "A data inicial deve ser anterior à data final.");
            consultas = List.of();
        } else {
            consultas = consultas.stream()
                    .filter(c -> inicio == null || !c.getDataHora().toLocalDate().isBefore(inicio))
                    .filter(c -> fim == null || !c.getDataHora().toLocalDate().isAfter(fim))
                    .filter(c -> status == null || c.getStatus() == status).toList();
        }
        mv.addObject("inicio", inicio);
        mv.addObject("fim", fim);
        mv.addObject("statusFiltro", status);
        mv.addObject("todosStatus", StatusConsulta.values());
        mv.addObject("consultas", consultas);
        mv.addObject("usuario", usuario);
        mv.addObject("mensagem", mensagem);
        if (!mv.getModel().containsKey("mensagemErro")) mv.addObject("mensagemErro", erro);
        return mv;
    }

    @GetMapping("/novo")
    public ModelAndView novo(HttpSession session,
                             @RequestParam(required = false) Long dentistaId,
                             @RequestParam(required = false) Long procedimentoId,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHora) {
        if (!podeOperar(session)) return negar();
        if (dentistaId == null || procedimentoId == null || dataHora == null) {
            return new ModelAndView("redirect:/agenda");
        }

        ConsultaFormDTO form = new ConsultaFormDTO();
        form.setDentistaId(dentistaId);
        form.setProcedimentoId(procedimentoId);
        form.setDataHora(dataHora);
        form.setStatus(StatusConsulta.AGENDADA);
        return formulario(form, null);
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid @ModelAttribute("consultaForm") ConsultaFormDTO form,
                               BindingResult result,
                               HttpSession session) {
        if (!podeOperar(session)) return negar();
        if (result.hasErrors()) return formulario(form, null);

        try {
            Consulta consulta = form.getId() == null ? new Consulta() : service.buscarPorId(form.getId());
            if (consulta.getId() != null && (consulta.getStatus() == StatusConsulta.REALIZADA ||
                    consulta.getStatus() == StatusConsulta.EM_ATENDIMENTO ||
                    consulta.getStatus() == StatusConsulta.AGUARDANDO))
                throw new IllegalArgumentException("Uma consulta em andamento ou realizada não pode ser alterada pela recepção.");
            if (form.getStatus() == StatusConsulta.REALIZADA || form.getStatus() == StatusConsulta.EM_ATENDIMENTO ||
                    form.getStatus() == StatusConsulta.AGUARDANDO)
                throw new IllegalArgumentException("Esse status é definido pelo fluxo do atendimento.");
            if (consulta.getId() != null) {
                if (!consulta.getPaciente().getId().equals(form.getPacienteId()) ||
                    !consulta.getProcedimento().getId().equals(form.getProcedimentoId()))
                    throw new IllegalArgumentException("Não é possível trocar paciente ou procedimento de uma consulta com cobrança.");
                if (form.getStatus() == StatusConsulta.CANCELADA) {
                    service.cancelar(consulta.getId());
                    return new ModelAndView("redirect:/consultas");
                }
            } else if (form.getStatus() == StatusConsulta.CANCELADA) {
                throw new IllegalArgumentException("Não é possível criar uma consulta cancelada.");
            }
            Paciente paciente = pacienteRepository.findById(form.getPacienteId())
                    .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado."));
            Dentista dentista = dentistaRepository.findById(form.getDentistaId())
                    .orElseThrow(() -> new IllegalArgumentException("Dentista não encontrado."));
            Procedimento procedimento = procedimentoRepository.findById(form.getProcedimentoId())
                    .orElseThrow(() -> new IllegalArgumentException("Procedimento não encontrado."));

            consulta.setPaciente(paciente);
            consulta.setDentista(dentista);
            consulta.setProcedimento(procedimento);
            consulta.setDataHora(form.getDataHora());
            consulta.setStatus(form.getStatus() == null ? StatusConsulta.AGENDADA : form.getStatus());
            consulta.setObservacoes(form.getObservacoes());
            service.salvar(consulta);

            return new ModelAndView("redirect:/agenda?data=" + consulta.getDataHora().toLocalDate()
                    + "&dentistaId=" + dentista.getId()
                    + "&procedimentoId=" + procedimento.getId());
        } catch (IllegalArgumentException e) {
            return formulario(form, e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!podeOperar(session)) return negar();
        Consulta consulta = service.buscarPorId(id);
        if (consulta.getStatus() == StatusConsulta.REALIZADA || consulta.getStatus() == StatusConsulta.EM_ATENDIMENTO
                || consulta.getStatus() == StatusConsulta.AGUARDANDO) {
            return new ModelAndView("redirect:/consultas?erro=Consultas+realizadas+não+podem+ser+alteradas+pela+recepção.");
        }
        if (consulta.getProcedimento() == null) {
            return new ModelAndView("redirect:/consultas?erro=Consulta+antiga+sem+procedimento.+Cancele+ou+exclua+e+faça+um+novo+agendamento+pela+agenda.");
        }

        ConsultaFormDTO form = new ConsultaFormDTO();
        form.setId(consulta.getId());
        form.setPacienteId(consulta.getPaciente().getId());
        form.setDentistaId(consulta.getDentista().getId());
        form.setProcedimentoId(consulta.getProcedimento().getId());
        form.setDataHora(consulta.getDataHora());
        form.setStatus(consulta.getStatus());
        form.setObservacoes(consulta.getObservacoes());
        return formulario(form, null);
    }

    @PostMapping("/cancelar/{id}")
    public ModelAndView cancelar(@PathVariable Long id, HttpSession session) {
        if (!podeOperar(session)) return negar();
        try {
            service.cancelar(id);
            return new ModelAndView("redirect:/consultas");
        } catch (IllegalArgumentException e) {
            return new ModelAndView("redirect:/consultas?erro="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!podeOperar(session)) return negar();
        try {
            service.excluir(id);
            return new ModelAndView("redirect:/consultas");
        } catch (IllegalArgumentException e) {
            return new ModelAndView("redirect:/consultas?erro="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private ModelAndView formulario(ConsultaFormDTO form, String mensagemErro) {
        ModelAndView mv = new ModelAndView("consultas/formulario");
        mv.addObject("consultaForm", form);
        mv.addObject("pacientes", pacienteRepository.findAll());
        mv.addObject("dentistaSelecionado", form.getDentistaId() == null ? null
                : dentistaRepository.findById(form.getDentistaId()).orElse(null));
        mv.addObject("procedimentoSelecionado", form.getProcedimentoId() == null ? null
                : procedimentoRepository.findById(form.getProcedimentoId()).orElse(null));
        mv.addObject("statusConsulta", List.of(StatusConsulta.AGENDADA, StatusConsulta.CONFIRMADA, StatusConsulta.CANCELADA));
        mv.addObject("mensagemErro", mensagemErro);
        return mv;
    }

    private boolean podeVisualizar(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.RECEPCIONISTA, Perfil.DENTISTA);
    }

    private boolean podeOperar(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.RECEPCIONISTA);
    }

    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
