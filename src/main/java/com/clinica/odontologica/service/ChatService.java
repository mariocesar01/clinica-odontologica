package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.*;
import com.clinica.odontologica.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {
    private final ConversaRepository conversas;
    private final MensagemRepository mensagens;
    private final PacienteRepository pacientes;
    public ChatService(ConversaRepository conversas, MensagemRepository mensagens, PacienteRepository pacientes) {
        this.conversas = conversas; this.mensagens = mensagens; this.pacientes = pacientes;
    }
    private Paciente pacienteDoUsuario(Usuario usuario) {
        return pacientes.findByUsuarioId(usuario.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Paciente sem cadastro vinculado."));
    }
    private void exigir(Usuario usuario) {
        if (usuario == null || (usuario.getPerfil() != Perfil.PACIENTE && usuario.getPerfil() != Perfil.RECEPCIONISTA))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido.");
    }
    @Transactional
    public Conversa obterOuCriar(Long pacienteId, Usuario usuario) {
        exigir(usuario);
        Paciente paciente = pacientes.findById(pacienteId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado."));
        if (usuario.getPerfil() == Perfil.PACIENTE && !paciente.getUsuario().getId().equals(usuario.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversa de outro paciente.");
        return conversas.findByPacienteId(pacienteId).orElseGet(() -> {
            Conversa conversa = new Conversa(); conversa.setPaciente(paciente); return conversas.save(conversa);
        });
    }
    @Transactional
    public Conversa minhaConversa(Usuario usuario) {
        exigir(usuario);
        if (usuario.getPerfil() != Perfil.PACIENTE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente pacientes.");
        return obterOuCriar(pacienteDoUsuario(usuario).getId(), usuario);
    }
    public List<Conversa> listar(Usuario usuario) {
        exigir(usuario);
        if (usuario.getPerfil() == Perfil.PACIENTE) return List.of(minhaConversa(usuario));
        return conversas.findAll();
    }
    private Conversa autorizada(Long conversaId, Usuario usuario) {
        exigir(usuario);
        Conversa c = conversas.findById(conversaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada."));
        if (usuario.getPerfil() == Perfil.PACIENTE && !c.getPaciente().getUsuario().getId().equals(usuario.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversa de outro paciente.");
        return c;
    }
    public List<Mensagem> mensagens(Long conversaId, Usuario usuario) {
        autorizada(conversaId, usuario);
        List<Mensagem> pagina = new ArrayList<>(mensagens.findByConversaIdOrderByEnviadaEmDesc(
                conversaId, PageRequest.of(0, 100)));
        Collections.reverse(pagina);
        return pagina;
    }
    @Transactional
    public Mensagem enviar(Long conversaId, String texto, Usuario usuario) {
        Conversa c = autorizada(conversaId, usuario);
        if (texto == null || texto.isBlank() || texto.strip().length() > 2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A mensagem deve ter entre 1 e 2000 caracteres.");
        Mensagem m = new Mensagem(); m.setConversa(c); m.setRemetente(usuario); m.setTexto(texto.strip());
        return mensagens.save(m);
    }
    @Transactional
    public int marcarLidas(Long conversaId, Usuario usuario) {
        autorizada(conversaId, usuario);
        List<Mensagem> naoLidas = mensagens.findByConversaIdAndLidaEmIsNullAndRemetenteIdNot(conversaId, usuario.getId());
        naoLidas.forEach(m -> m.setLidaEm(LocalDateTime.now()));
        return naoLidas.size();
    }
}
