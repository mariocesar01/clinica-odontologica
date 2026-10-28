package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Dentista;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.DentistaRepository;
import com.clinica.odontologica.repository.EscalaDentistaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DentistaService {

    private final DentistaRepository dentistaRepository;
    private final UsuarioService usuarioService;
    private final ConsultaRepository consultaRepository;
    private final EscalaDentistaRepository escalaRepository;

    public DentistaService(DentistaRepository dentistaRepository,
                           UsuarioService usuarioService,
                           ConsultaRepository consultaRepository,
                           EscalaDentistaRepository escalaRepository) {
        this.dentistaRepository = dentistaRepository;
        this.usuarioService = usuarioService;
        this.consultaRepository = consultaRepository;
        this.escalaRepository = escalaRepository;
    }

    public List<Dentista> listarTodos() { return dentistaRepository.findAll(); }

    public Dentista buscarPorId(Long id) {
        return dentistaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dentista não encontrado."));
    }

    public List<Dentista> pesquisarPorNome(String nome) {
        return dentistaRepository.findByUsuarioNomeContainingIgnoreCase(nome);
    }

    @Transactional
    public Dentista salvar(Dentista dentista) {
        validarCroDuplicado(dentista);
        Usuario usuario = dentista.getUsuario();
        usuario.setPerfil(Perfil.DENTISTA);
        Usuario usuarioSalvo = usuarioService.salvar(usuario);
        dentista.setUsuario(usuarioSalvo);
        return dentistaRepository.save(dentista);
    }

    @Transactional
    public void excluir(Long id) {
        if (consultaRepository.existsByDentistaId(id)) {
            throw new IllegalArgumentException("Não é possível excluir um dentista que possui consultas cadastradas.");
        }
        if (escalaRepository.existsByDentistaId(id)) {
            throw new IllegalArgumentException("Não é possível excluir um dentista que possui escalas cadastradas.");
        }

        Dentista dentista = buscarPorId(id);
        Long usuarioId = dentista.getUsuario().getId();
        dentistaRepository.delete(dentista);
        dentistaRepository.flush();
        usuarioService.excluir(usuarioId);
    }

    private void validarCroDuplicado(Dentista dentista) {
        dentistaRepository.findByCroIgnoreCase(dentista.getCro()).ifPresent(existente -> {
            if (dentista.getId() == null || !existente.getId().equals(dentista.getId())) {
                throw new IllegalArgumentException("Já existe um dentista cadastrado com este CRO.");
            }
        });
    }
}
