package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class SessaoService {

    public Usuario usuarioLogado(HttpSession session) {
        Object usuario = session.getAttribute("usuarioLogado");
        return usuario instanceof Usuario ? (Usuario) usuario : null;
    }

    public boolean possuiPerfil(HttpSession session, Perfil... perfisPermitidos) {
        Usuario usuario = usuarioLogado(session);
        if (usuario == null) {
            return false;
        }
        return Arrays.asList(perfisPermitidos).contains(usuario.getPerfil());
    }
}
