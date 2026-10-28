package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Conversa;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.ConversaRepository;
import com.clinica.odontologica.repository.PacienteRepository;
import com.clinica.odontologica.service.ChatService;
import com.clinica.odontologica.service.SessaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;

@Controller
public class ChatTelaController {
    private final ChatService chat;
    private final SessaoService sessao;
    private final ConversaRepository conversas;
    private final PacienteRepository pacientes;

    public ChatTelaController(ChatService chat, SessaoService sessao,
                              ConversaRepository conversas, PacienteRepository pacientes) {
        this.chat = chat;
        this.sessao = sessao;
        this.conversas = conversas;
        this.pacientes = pacientes;
    }

    private Usuario usuario(HttpSession session) {
        Usuario u = sessao.usuarioLogado(session);
        if (u == null || (u.getPerfil() != Perfil.PACIENTE && u.getPerfil() != Perfil.RECEPCIONISTA)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido.");
        }
        return u;
    }

    @GetMapping("/chat")
    public String inicio(HttpSession session) {
        Usuario u = usuario(session);
        return u.getPerfil() == Perfil.PACIENTE ? "redirect:/chat/minha" : "redirect:/chat/conversas";
    }

    @GetMapping("/chat/minha")
    public String minha(HttpSession session) {
        Usuario u = usuario(session);
        if (u.getPerfil() != Perfil.PACIENTE) return "redirect:/acesso-negado";
        return "redirect:/chat/conversas/" + chat.minhaConversa(u).getId();
    }

    @GetMapping("/chat/conversas")
    public ModelAndView listar(HttpSession session) {
        Usuario u = usuario(session);
        if (u.getPerfil() != Perfil.RECEPCIONISTA) return new ModelAndView("redirect:/acesso-negado");
        ModelAndView mv = new ModelAndView("chat/lista");
        mv.addObject("conversas", chat.listar(u).stream()
                .sorted(Comparator.comparing(c -> c.getPaciente().getUsuario().getNome().toLowerCase())).toList());
        mv.addObject("pacientes", pacientes.findAll().stream()
                .sorted(Comparator.comparing(p -> p.getUsuario().getNome().toLowerCase())).toList());
        return mv;
    }

    @PostMapping("/chat/conversas/iniciar")
    public String iniciar(@RequestParam Long pacienteId, HttpSession session) {
        Usuario u = usuario(session);
        if (u.getPerfil() != Perfil.RECEPCIONISTA) return "redirect:/acesso-negado";
        return "redirect:/chat/conversas/" + chat.obterOuCriar(pacienteId, u).getId();
    }

    @GetMapping("/chat/conversas/{id}")
    public ModelAndView visualizar(@PathVariable Long id, HttpSession session) {
        Usuario u = usuario(session);
        // O serviço valida se esta conversa pertence ao paciente conectado.
        chat.marcarLidas(id, u);
        var mensagens = chat.mensagens(id, u);
        Conversa conversa = conversas.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada."));
        ModelAndView mv = new ModelAndView("chat/conversa");
        mv.addObject("conversa", conversa);
        mv.addObject("mensagens", mensagens);
        mv.addObject("usuario", u);
        mv.addObject("recepcao", u.getPerfil() == Perfil.RECEPCIONISTA);
        return mv;
    }

    @PostMapping("/chat/conversas/{id}/mensagens")
    public String enviar(@PathVariable Long id, @RequestParam String texto,
                         HttpSession session, RedirectAttributes redirect) {
        Usuario u = usuario(session);
        try {
            chat.enviar(id, texto, u);
        } catch (ResponseStatusException erro) {
            // Mensagens inválidas não derrubam a tela; acesso negado não é ocultado.
            if (erro.getStatusCode().value() != 400) throw erro;
            redirect.addFlashAttribute("erro", erro.getReason());
        }
        return "redirect:/chat/conversas/" + id;
    }
}
