package com.clinica.odontologica.controller;

import com.clinica.odontologica.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Dados mínimos de identificação disponíveis nos templates Thymeleaf. */
@ControllerAdvice
public class UsuarioAtualControllerAdvice {
    @ModelAttribute("usuarioAtual")
    public Usuario usuarioAtual(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null && sessao.getAttribute("usuarioLogado") instanceof Usuario usuario) return usuario;
        return null;
    }
}
