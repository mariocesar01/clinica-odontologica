package com.clinica.odontologica.controller;

import com.clinica.odontologica.model.*;
import com.clinica.odontologica.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatService chat;
    private final SessaoService sessao;
    public ChatApiController(ChatService chat, SessaoService sessao) { this.chat = chat; this.sessao = sessao; }
    private Usuario usuario(HttpSession session) {
        Usuario u = sessao.usuarioLogado(session);
        if (u == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login.");
        return u;
    }
    public record ConversaDTO(Long id, Long pacienteId, String paciente, LocalDateTime criadaEm) {
        public static ConversaDTO de(Conversa c) { return new ConversaDTO(c.getId(), c.getPaciente().getId(),
                c.getPaciente().getUsuario().getNome(), c.getCriadaEm()); }
    }
    public record MensagemDTO(Long id, Long conversaId, Long remetenteId, String remetente,
                              String texto, LocalDateTime enviadaEm, LocalDateTime lidaEm) {
        public static MensagemDTO de(Mensagem m) { return new MensagemDTO(m.getId(), m.getConversa().getId(),
                m.getRemetente().getId(), m.getRemetente().getNome(), m.getTexto(), m.getEnviadaEm(), m.getLidaEm()); }
    }
    public record EnviarMensagem(String texto) {}
    @GetMapping("/conversas")
    public List<ConversaDTO> conversas(HttpSession session) {
        return chat.listar(usuario(session)).stream().map(ConversaDTO::de).toList();
    }
    @GetMapping("/minha-conversa")
    public ConversaDTO minha(HttpSession session) { return ConversaDTO.de(chat.minhaConversa(usuario(session))); }
    @PostMapping("/conversas/paciente/{pacienteId}")
    public ConversaDTO iniciar(@PathVariable Long pacienteId, HttpSession session) {
        return ConversaDTO.de(chat.obterOuCriar(pacienteId, usuario(session)));
    }
    @GetMapping("/conversas/{id}/mensagens")
    public List<MensagemDTO> mensagens(@PathVariable Long id, HttpSession session) {
        return chat.mensagens(id, usuario(session)).stream().map(MensagemDTO::de).toList();
    }
    @PostMapping("/conversas/{id}/mensagens")
    public MensagemDTO enviar(@PathVariable Long id, @RequestBody EnviarMensagem body, HttpSession session) {
        return MensagemDTO.de(chat.enviar(id, body.texto(), usuario(session)));
    }
    @PostMapping("/conversas/{id}/ler")
    public Map<String, Integer> marcarLidas(@PathVariable Long id, HttpSession session) {
        return Map.of("marcadasComoLidas", chat.marcarLidas(id, usuario(session)));
    }
}
