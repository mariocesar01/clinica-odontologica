package com.clinica.odontologica.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import com.clinica.odontologica.repository.UsuarioRepository;
import com.clinica.odontologica.model.Usuario;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final UsuarioRepository usuarios;
    public LoginInterceptor(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("usuarioLogado") instanceof Usuario anterior
                && anterior.getId() != null) {
            Usuario atual = usuarios.findById(anterior.getId()).orElse(null);
            if (atual != null && Boolean.TRUE.equals(atual.getAtivo())) {
                session.setAttribute("usuarioLogado", atual);
                return true;
            }
            session.invalidate();
        }

        if (request.getRequestURI().startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Faça login.");
        } else {
            response.sendRedirect("/");
        }
        return false;
    }
}
