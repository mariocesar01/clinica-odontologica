package com.clinica.odontologica.controller;

import com.clinica.odontologica.dto.HorarioAgendaDTO;
import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.Consulta;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.EscalaDentista;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.ProcedimentoRepository;
import com.clinica.odontologica.service.AgendaService;
import com.clinica.odontologica.service.EscalaDentistaService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;
    private final EscalaDentistaService escalaService;
    private final DentistaRepository dentistaRepository;
    private final ConsultaRepository consultaRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final SessaoService sessaoService;

    public AgendaController(AgendaService agendaService,
                            EscalaDentistaService escalaService,
                            DentistaRepository dentistaRepository,
                            ConsultaRepository consultaRepository,
                            ProcedimentoRepository procedimentoRepository,
                            SessaoService sessaoService) {
        this.agendaService = agendaService;
        this.escalaService = escalaService;
        this.dentistaRepository = dentistaRepository;
        this.consultaRepository = consultaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView agenda(@RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                               @RequestParam(required = false) Long dentistaId,
                               @RequestParam(required = false) Long procedimentoId,
                               @RequestParam(required = false) String erro,
                               HttpSession session) {
        if (!permitido(session)) return new ModelAndView("redirect:/acesso-negado");

        Usuario usuario = sessaoService.usuarioLogado(session);
        LocalDate dataSelecionada = data == null ? LocalDate.now() : data;
        List<EscalaDentista> escalasDoDia = escalasVisiveis(usuario, dataSelecionada);

        ModelAndView mv = new ModelAndView("agenda/index");
        mv.addObject("usuario", usuario);
        mv.addObject("dataSelecionada", dataSelecionada);
        mv.addObject("escalasDoDia", escalasDoDia);
        mv.addObject("procedimentos", procedimentoRepository.findByAtivoTrueOrderByNomeAsc());
        mv.addObject("procedimentoSelecionadoId", procedimentoId);
        mv.addObject("podeAtenderData", dataSelecionada.equals(LocalDate.now()));
        if (erro != null && !erro.isBlank()) {
            mv.addObject("mensagemErro", erro);
        }

        Long selecionado = selecionarDentistaValido(usuario, dentistaId, escalasDoDia);
        mv.addObject("dentistaSelecionadoId", selecionado);

        if (usuario.getPerfil() == Perfil.DENTISTA && selecionado != null) {
            List<Consulta> consultasDoDia =
                    consultaRepository.findByDentistaIdAndDataHoraBetweenOrderByDataHoraAsc(
                            selecionado, dataSelecionada.atStartOfDay(), dataSelecionada.plusDays(1).atStartOfDay())
                            .stream()
                            .filter(c -> c.getStatus() == StatusConsulta.AGENDADA
                                    || c.getStatus() == StatusConsulta.CONFIRMADA
                                    || c.getStatus() == StatusConsulta.AGUARDANDO
                                    || c.getStatus() == StatusConsulta.EM_ATENDIMENTO)
                            .toList();
            mv.addObject("consultasDoDia", consultasDoDia);
        }

        if (usuario.getPerfil() == Perfil.RECEPCIONISTA && selecionado != null) {
            // Cada consulta aparece uma vez, independentemente de sua duracao.
            List<Consulta> consultasDoDia = consultaRepository
                    .findByDentistaIdAndDataHoraBetweenOrderByDataHoraAsc(
                            selecionado, dataSelecionada.atStartOfDay(),
                            dataSelecionada.plusDays(1).atStartOfDay())
                    .stream()
                    .filter(c -> c.getStatus() != StatusConsulta.CANCELADA)
                    .toList();
            mv.addObject("consultasMarcadas", consultasDoDia);

            if (procedimentoId != null) {
                try {
                    // Exibe somente as alternativas validas para um novo agendamento.
                    // Horarios passados e candidatos que conflitam com consultas
                    // nao devem aparecer como multiplos compromissos.
                    List<HorarioAgendaDTO> horariosDisponiveis = agendaService
                            .montarAgenda(selecionado, dataSelecionada, procedimentoId)
                            .stream()
                            .filter(HorarioAgendaDTO::isDisponivel)
                            .toList();
                    mv.addObject("horariosDisponiveis", horariosDisponiveis);
                } catch (IllegalArgumentException e) {
                    mv.addObject("mensagemErro", e.getMessage());
                }
            }
        }

        return mv;
    }

    private List<EscalaDentista> escalasVisiveis(Usuario usuario, LocalDate data) {
        if (usuario.getPerfil() == Perfil.RECEPCIONISTA) {
            return escalaService.listarAtivasPorData(data);
        }

        Dentista dentista = dentistaRepository.findByUsuarioId(usuario.getId()).orElse(null);
        if (dentista == null) return List.of();
        return escalaService.buscarEscalaAtiva(dentista.getId(), data)
                .map(e -> List.of(e))
                .orElseGet(List::of);
    }

    private Long selecionarDentistaValido(Usuario usuario,
                                           Long solicitado,
                                           List<EscalaDentista> escalas) {
        if (usuario.getPerfil() == Perfil.DENTISTA) {
            return escalas.isEmpty() ? null : escalas.get(0).getDentista().getId();
        }

        if (solicitado == null) return null;
        return escalas.stream()
                .map(EscalaDentista::getDentista)
                .map(Dentista::getId)
                .filter(id -> id.equals(solicitado))
                .findFirst()
                .orElse(null);
    }

    private boolean permitido(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.RECEPCIONISTA, Perfil.DENTISTA);
    }
}
