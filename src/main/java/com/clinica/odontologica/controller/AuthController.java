package com.clinica.odontologica.controller;

import com.clinica.odontologica.dto.LoginDTO;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.service.UsuarioService;
import com.clinica.odontologica.service.PainelService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final PainelService painelService;

    public AuthController(UsuarioService usuarioService, PainelService painelService) {
        this.usuarioService = usuarioService;
        this.painelService = painelService;
    }

    @GetMapping("/")
    public ModelAndView login(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null && sessao.getAttribute("usuarioLogado") instanceof Usuario) {
            return new ModelAndView("redirect:/home");
        }
        ModelAndView mv = new ModelAndView("login/login");
        mv.addObject("login", new LoginDTO());
        return mv;
    }

    @PostMapping("/login")
    public ModelAndView login(@Valid LoginDTO login, BindingResult result, HttpSession session, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView("login/login");

        if (result.hasErrors()) {
            mv.addObject("login", login);
            return mv;
        }

        Optional<Usuario> usuario = usuarioService.autenticar(login.getEmail(), login.getSenha());

        if (usuario.isEmpty()) {
            mv.addObject("login", login);
            mv.addObject("mensagemErro", "E-mail ou senha inválidos.");
            return mv;
        }

        request.changeSessionId(); // Evita reutilizar o identificador de sessão anterior ao login.
        session.setAttribute("usuarioLogado", usuario.get());
        return new ModelAndView("redirect:/home");
    }

    @GetMapping("/home")
    public ModelAndView home(HttpSession session) {
        ModelAndView mv = new ModelAndView("home/index");
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        mv.addObject("usuario", usuario);
        mv.addObject("painel", painelService.obter(usuario));
        return mv;
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
