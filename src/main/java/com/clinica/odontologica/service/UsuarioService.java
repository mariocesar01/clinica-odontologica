package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarAdministradores() {
        return usuarioRepository.findByPerfilIn(List.of(Perfil.ADMINISTRADOR));
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Transactional
    public Usuario salvar(Usuario usuario) {
        validarDadosBasicos(usuario);
        validarEmailDuplicado(usuario);

        if (usuario.getId() == null) {
            validarSenhaNova(usuario.getSenha());
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            if (usuario.getAtivo() == null) {
                usuario.setAtivo(true);
            }
        } else {
            Usuario existente = usuarioRepository.findById(usuario.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

            if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
                usuario.setSenha(existente.getSenha());
            } else if (!usuario.getSenha().startsWith("$2")) {
                validarSenhaNova(usuario.getSenha());
                usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            }

            if (usuario.getAtivo() == null) {
                usuario.setAtivo(existente.getAtivo());
            }
            if (usuario.getPerfil() == null) {
                usuario.setPerfil(existente.getPerfil());
            }
        }

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> autenticar(String email, String senha) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                .filter(usuario -> passwordEncoder.matches(senha, usuario.getSenha()));
    }

    public void excluir(Long id) {
        usuarioRepository.deleteById(id);
    }

    private void validarDadosBasicos(Usuario usuario) {
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome do usuário é obrigatório.");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("O e-mail do usuário é obrigatório.");
        }
        if (usuario.getPerfil() == null) {
            throw new IllegalArgumentException("O perfil do usuário é obrigatório.");
        }
    }

    private void validarSenhaNova(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve possuir pelo menos 8 caracteres.");
        }
    }

    private void validarEmailDuplicado(Usuario usuario) {
        usuarioRepository.findByEmailIgnoreCase(usuario.getEmail()).ifPresent(existente -> {
            if (usuario.getId() == null || !existente.getId().equals(usuario.getId())) {
                throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
            }
        });
    }
}
