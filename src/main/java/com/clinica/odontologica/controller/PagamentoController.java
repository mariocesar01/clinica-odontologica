package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.FormaPagamento;
import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.enums.StatusPagamento;
import com.clinica.odontologica.service.PagamentoService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/pagamentos")
public class PagamentoController {
    private final PagamentoService pagamentos;
    private final SessaoService sessao;
    public PagamentoController(PagamentoService pagamentos, SessaoService sessao) {
        this.pagamentos = pagamentos;
        this.sessao = sessao;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session, @RequestParam(required=false) String mensagem,
                               @RequestParam(required=false) String erro,
                               @RequestParam(required=false) StatusPagamento status) {
        if (!permitido(session)) return negar();
        ModelAndView mv = new ModelAndView("pagamentos/lista");
        var cobrancas = pagamentos.listarTodos().stream()
                .filter(c -> status == null || c.getStatus() == status).toList();
        var temTentativa = new java.util.HashMap<Long, Boolean>();
        for (var c : cobrancas) temTentativa.put(c.getId(), pagamentos.temTentativaOnline(c.getId()));
        mv.addObject("pagamentos", cobrancas);
        mv.addObject("statusFiltro", status);
        mv.addObject("todosStatus", StatusPagamento.values());
        mv.addObject("temTentativaOnline", temTentativa);
        mv.addObject("formasPagamento", FormaPagamento.values());
        mv.addObject("mensagem", mensagem);
        mv.addObject("mensagemErro", erro);
        return mv;
    }

    @PostMapping("/registrar/{id}")
    public ModelAndView registrar(@PathVariable Long id,
                                 @RequestParam(required=false) FormaPagamento formaPagamento,
                                 HttpSession session) {
        if (!permitido(session)) return negar();
        try {
            pagamentos.registrarPresencial(id, formaPagamento);
            return new ModelAndView("redirect:/pagamentos?mensagem=" + codificar("Pagamento presencial registrado."));
        } catch (IllegalArgumentException e) {
            return new ModelAndView("redirect:/pagamentos?erro=" + codificar(e.getMessage()));
        }
    }

    // URLs do antigo CRUD não podem mais criar, editar ou excluir cobranças.
    @GetMapping({"/novo", "/editar/{id}"})
    public ModelAndView paginaAntiga(HttpSession session) {
        if (!permitido(session)) return negar();
        return new ModelAndView("redirect:/pagamentos");
    }
    @PostMapping({"/salvar", "/pagar/{id}", "/excluir/{id}"})
    public ModelAndView acaoAntiga(HttpSession session) {
        if (!permitido(session)) return negar();
        return new ModelAndView("redirect:/pagamentos?erro=" +
                codificar("As cobranças são geradas automaticamente. Use Registrar pagamento presencial."));
    }

    private String codificar(String texto) { return URLEncoder.encode(texto, StandardCharsets.UTF_8); }
    private boolean permitido(HttpSession session) {
        return sessao.possuiPerfil(session, Perfil.RECEPCIONISTA);
    }
    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
