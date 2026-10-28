package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Recepcionista;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.RecepcionistaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecepcionistaService {

    private final RecepcionistaRepository repository;
    private final UsuarioService usuarioService;

    public RecepcionistaService(RecepcionistaRepository repository, UsuarioService usuarioService) {
        this.repository = repository;
        this.usuarioService = usuarioService;
    }

    public List<Recepcionista> listarTodos() { return repository.findAll(); }

    public Recepcionista buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recepcionista não encontrado."));
    }

    public List<Recepcionista> pesquisarPorNome(String nome) {
        return repository.findByUsuarioNomeContainingIgnoreCase(nome);
    }

    @Transactional
    public Recepcionista salvar(Recepcionista recepcionista) {
        normalizarCpf(recepcionista);
        validarCpfDuplicado(recepcionista);

        Usuario usuario = recepcionista.getUsuario();
        usuario.setPerfil(Perfil.RECEPCIONISTA);
        Usuario usuarioSalvo = usuarioService.salvar(usuario);
        recepcionista.setUsuario(usuarioSalvo);

        return repository.save(recepcionista);
    }

    @Transactional
    public void excluir(Long id) {
        Recepcionista recepcionista = buscarPorId(id);
        Long usuarioId = recepcionista.getUsuario().getId();
        repository.delete(recepcionista);
        repository.flush();
        usuarioService.excluir(usuarioId);
    }

    private void normalizarCpf(Recepcionista recepcionista) {
        if (recepcionista.getCpf() != null) {
            recepcionista.setCpf(recepcionista.getCpf().replaceAll("\\D", ""));
        }
    }

    private void validarCpfDuplicado(Recepcionista recepcionista) {
        repository.findByCpf(recepcionista.getCpf()).ifPresent(existente -> {
            if (recepcionista.getId() == null || !existente.getId().equals(recepcionista.getId())) {
                throw new IllegalArgumentException("Já existe uma recepcionista cadastrada com este CPF.");
            }
        });
    }
}
