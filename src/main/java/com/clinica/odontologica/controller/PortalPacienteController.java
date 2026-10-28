package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.*;
import com.clinica.odontologica.service.MercadoPagoClient;
import com.clinica.odontologica.service.PagamentoOnlineService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class PortalPacienteController {
    private final PagamentoOnlineService pagamentos;
    private final SessaoService sessao;
    private final MercadoPagoClient mercadoPago;

    public PortalPacienteController(PagamentoOnlineService pagamentos, SessaoService sessao,
                                    MercadoPagoClient mercadoPago) {
        this.pagamentos = pagamentos;
        this.sessao = sessao;
        this.mercadoPago = mercadoPago;
    }

    public record LinhaConsulta(Long id, LocalDateTime dataHora, String dentista,
                                String procedimento, BigDecimal valor, String status,
                                String pagamento, boolean podePagar) {}

    private Usuario usuario(HttpSession session) {
        Usuario u = sessao.usuarioLogado(session);
        if (u == null || u.getPerfil() != Perfil.PACIENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente pacientes.");
        }
        return u;
    }

    private List<LinhaConsulta> linhas(Usuario u) {
        return pagamentos.minhasConsultas(u).stream().map(c -> {
            Map<String, Object> cobranca = pagamentos.statusDaConsulta(c.getId(), u);
            String statusPag = String.valueOf(cobranca.get("status"));
            BigDecimal valor = c.getValorProcedimento() != null ? c.getValorProcedimento()
                    : c.getProcedimento() == null ? BigDecimal.ZERO : c.getProcedimento().getValor();
            boolean pagar = c.getStatus() != StatusConsulta.CANCELADA
                    && c.getProcedimento() != null && valor != null && valor.signum() > 0
                    && !statusPag.equals("PAGO") && !statusPag.equals("CANCELADO");
            return new LinhaConsulta(c.getId(), c.getDataHora(), c.getDentista().getUsuario().getNome(),
                    c.getProcedimento() == null ? "Não informado" : c.getProcedimento().getNome(),
                    valor, c.getStatus().name(), statusPag, pagar);
        }).toList();
    }

    @GetMapping("/paciente/consultas")
    public ModelAndView consultas(HttpSession session) {
        ModelAndView mv = new ModelAndView("portal-paciente/consultas");
        mv.addObject("consultas", linhas(usuario(session)));
        return mv;
    }

    @GetMapping("/paciente/pagamentos")
    public ModelAndView meusPagamentos(HttpSession session) {
        ModelAndView mv = new ModelAndView("portal-paciente/pagamentos");
        Usuario u = usuario(session);
        mv.addObject("consultas", linhas(u));
        mv.addObject("pagamentoConfigurado", mercadoPago.configurado());
        return mv;
    }

    @GetMapping("/paciente/pagamentos/consulta/{id}")
    public ModelAndView detalhes(@PathVariable Long id, HttpSession session) {
        Usuario u = usuario(session);
        Consulta consulta = pagamentos.consultaMinha(id, u);
        ModelAndView mv = new ModelAndView("portal-paciente/detalhes-pagamento");
        mv.addObject("consulta", consulta);
        mv.addObject("statusPagamento", pagamentos.statusDaConsulta(id, u));
        mv.addObject("tentativas", pagamentos.tentativasMinhas(id, u));
        mv.addObject("pagamentoConfigurado", mercadoPago.configurado());
        return mv;
    }

    @PostMapping("/paciente/consultas/{id}/pagar")
    public String pagar(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        Usuario u = usuario(session);
        try {
            PagamentoOnlineService.Checkout checkout = pagamentos.iniciar(id, u);
            URI destino = URI.create(checkout.url());
            String host = destino.getHost();
            if (!"https".equalsIgnoreCase(destino.getScheme()) || host == null ||
                !(host.equals("mercadopago.com") || host.endsWith(".mercadopago.com") ||
                  host.equals("mercadopago.com.br") || host.endsWith(".mercadopago.com.br"))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Endereço do checkout inválido.");
            }
            return "redirect:" + destino.toASCIIString();
        } catch (ResponseStatusException | IllegalArgumentException erro) {
            String mensagem = erro instanceof ResponseStatusException re && re.getReason() != null
                    ? re.getReason() : "Não foi possível iniciar o pagamento.";
            redirect.addFlashAttribute("erro", mensagem);
            return "redirect:/paciente/pagamentos/consulta/" + id;
        }
    }

    @PostMapping("/paciente/tentativas/{id}/verificar")
    public String verificar(@PathVariable Long id, @RequestParam Long consultaId,
                            HttpSession session, RedirectAttributes redirect) {
        Usuario u = usuario(session);
        // O serviço valida a titularidade da tentativa e da consulta.
        try {
            pagamentos.consultaMinha(consultaId, u);
            pagamentos.verificarTentativa(id, u);
            redirect.addFlashAttribute("mensagem", "Situação consultada. A aprovação depende da confirmação do provedor.");
        } catch (ResponseStatusException erro) {
            if (erro.getStatusCode().value() == 403) throw erro;
            redirect.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/paciente/pagamentos/consulta/" + consultaId;
    }

    @GetMapping("/paciente/meus-dados")
    public ModelAndView meusDados(HttpSession session) {
        ModelAndView mv = new ModelAndView("portal-paciente/meus-dados");
        mv.addObject("paciente", pagamentos.meuPaciente(usuario(session)));
        return mv;
    }
}
