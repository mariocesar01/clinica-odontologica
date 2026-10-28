package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.service.SessaoService;
import com.clinica.odontologica.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final SessaoService sessaoService;

    public UsuarioController(UsuarioService usuarioService, SessaoService sessaoService) {
        this.usuarioService = usuarioService;
        this.sessaoService = sessaoService;
    }

    @GetMapping
    public ModelAndView listar(HttpSession session,
                               @RequestParam(required = false) String mensagem,
                               @RequestParam(required = false) String erro) {
        if (!administrador(session)) return negar();
        ModelAndView mv = new ModelAndView("usuarios/lista");
        mv.addObject("usuarios", usuarioService.listarAdministradores());
        mv.addObject("usuarioLogado", sessaoService.usuarioLogado(session));
        mv.addObject("mensagem", mensagem);
        mv.addObject("mensagemErro", erro);
        return mv;
    }

    @GetMapping("/novo")
    public ModelAndView novo(HttpSession session) {
        if (!administrador(session)) return negar();
        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfil.ADMINISTRADOR);
        usuario.setAtivo(true);
        return formulario(usuario, null);
    }

    @PostMapping("/salvar")
    public ModelAndView salvar(@Valid Usuario usuario, BindingResult result, HttpSession session) {
        if (!administrador(session)) return negar();
        if (result.hasErrors()) return formulario(usuario, null);

        usuario.setPerfil(Perfil.ADMINISTRADOR);

        try {
            usuarioService.salvar(usuario);
            return new ModelAndView("redirect:/usuarios");
        } catch (IllegalArgumentException e) {
            return formulario(usuario, e.getMessage());
        }
    }

    @GetMapping("/editar/{id}")
    public ModelAndView editar(@PathVariable Long id, HttpSession session) {
        if (!administrador(session)) return negar();
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        usuario.setSenha("");
        return formulario(usuario, null);
    }

    @PostMapping("/excluir/{id}")
    public ModelAndView excluir(@PathVariable Long id, HttpSession session) {
        if (!administrador(session)) return negar();
        Usuario logado = sessaoService.usuarioLogado(session);
        if (logado != null && logado.getId().equals(id)) {
            ModelAndView mv = listar(session, null, null);
            mv.addObject("mensagemErro", "Não é possível excluir o usuário que está logado.");
            return mv;
        }
        usuarioService.excluir(id);
        return new ModelAndView("redirect:/usuarios");
    }

    private ModelAndView formulario(Usuario usuario, String mensagemErro) {
        ModelAndView mv = new ModelAndView("usuarios/formulario");
        mv.addObject("usuario", usuario);
        mv.addObject("mensagemErro", mensagemErro);
        return mv;
    }

    private boolean administrador(HttpSession session) {
        return sessaoService.possuiPerfil(session, Perfil.ADMINISTRADOR);
    }

    private ModelAndView negar() { return new ModelAndView("redirect:/acesso-negado"); }
}
