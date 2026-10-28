package com.clinica.odontologica.controller;

import com.clinica.odontologica.model.*;
import com.clinica.odontologica.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PagamentoOnlineController {
    private final PagamentoOnlineService pagamentos;
    private final SessaoService sessao;
    private final AssinaturaWebhookService assinaturas;
    public PagamentoOnlineController(PagamentoOnlineService pagamentos, SessaoService sessao,
                                     AssinaturaWebhookService assinaturas) {
        this.pagamentos = pagamentos; this.sessao = sessao; this.assinaturas = assinaturas;
    }
    private Usuario usuario(HttpSession session) { return sessao.usuarioLogado(session); }
    public record ConsultaDTO(Long id, String data, String status, String procedimento, String valor) {
        public static ConsultaDTO de(Consulta c) { return new ConsultaDTO(c.getId(), c.getDataHora().toString(),
                c.getStatus().name(), c.getProcedimento() == null ? "" : c.getProcedimento().getNome(),
                c.getValorProcedimento() == null ? (c.getProcedimento() == null ? "" : c.getProcedimento().getValor().toPlainString()) : c.getValorProcedimento().toPlainString()); }
    }
    public record TentativaDTO(Long id, String status, String urlCheckout, String criadaEm) {
        public static TentativaDTO de(TentativaPagamentoOnline t) {
            return new TentativaDTO(t.getId(), t.getStatus().name(), t.getUrlCheckout(), t.getCriadaEm().toString());
        }
    }
    @GetMapping("/paciente/consultas")
    public List<ConsultaDTO> consultas(HttpSession session) {
        return pagamentos.minhasConsultas(usuario(session)).stream().map(ConsultaDTO::de).toList();
    }
    @GetMapping("/paciente/consultas/{consultaId}/pagamento")
    public Map<String, Object> statusPagamento(@PathVariable Long consultaId, HttpSession session) {
        return pagamentos.statusDaConsulta(consultaId, usuario(session));
    }
    @PostMapping("/paciente/consultas/{consultaId}/checkout")
    public PagamentoOnlineService.Checkout checkout(@PathVariable Long consultaId, HttpSession session) {
        return pagamentos.iniciar(consultaId, usuario(session));
    }
    @GetMapping("/paciente/consultas/{consultaId}/tentativas")
    public List<TentativaDTO> tentativas(@PathVariable Long consultaId, HttpSession session) {
        return pagamentos.tentativasMinhas(consultaId, usuario(session)).stream().map(TentativaDTO::de).toList();
    }
    @PostMapping("/paciente/tentativas/{tentativaId}/verificar")
    public Map<String, String> verificar(@PathVariable Long tentativaId, HttpSession session) {
        pagamentos.verificarTentativa(tentativaId, usuario(session));
        return Map.of("mensagem", "Consulta ao provedor concluída; acompanhe o status da tentativa.");
    }
    @PostMapping("/pagamentos/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader(value = "x-signature", required = false) String signature,
                        @RequestHeader(value = "x-request-id", required = false) String requestId,
                        @RequestParam(value = "data.id", required = false) String idQuery,
                        @RequestBody(required = false) Map<String, Object> body) {
        if (!assinaturas.assinaturaValida(signature, requestId, idQuery))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura inválida.");
        Object data = body == null ? null : body.get("data");
        String idBody = data instanceof Map<?,?> m ? String.valueOf(m.get("id")) : "";
        if (!Objects.equals(idQuery, idBody))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificadores divergentes.");
        String tipo = body == null ? "" : String.valueOf(body.get("type"));
        if ("payment".equals(tipo)) pagamentos.atualizarPorPagamentoProvedor(idQuery);
        return ResponseEntity.ok().build();
    }
}
