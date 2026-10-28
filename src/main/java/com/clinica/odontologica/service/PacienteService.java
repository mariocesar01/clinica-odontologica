package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Paciente;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UsuarioService usuarioService;
    private final ConsultaRepository consultaRepository;

    public PacienteService(PacienteRepository pacienteRepository,
                           UsuarioService usuarioService,
                           ConsultaRepository consultaRepository) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioService = usuarioService;
        this.consultaRepository = consultaRepository;
    }

    public List<Paciente> listarTodos() { return pacienteRepository.findAll(); }

    public Paciente buscarPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado."));
    }

    public List<Paciente> pesquisarPorNome(String nome) {
        return pacienteRepository.findByUsuarioNomeContainingIgnoreCase(nome);
    }

    @Transactional
    public Paciente salvar(Paciente paciente) {
        normalizarCpf(paciente);
        validarCpfDuplicado(paciente);

        Usuario usuario = paciente.getUsuario();
        usuario.setPerfil(Perfil.PACIENTE);
        Usuario usuarioSalvo = usuarioService.salvar(usuario);
        paciente.setUsuario(usuarioSalvo);

        return pacienteRepository.save(paciente);
    }

    @Transactional
    public void excluir(Long id) {
        if (consultaRepository.existsByPacienteId(id)) {
            throw new IllegalArgumentException("Não é possível excluir um paciente que possui consultas cadastradas.");
        }

        Paciente paciente = buscarPorId(id);
        Long usuarioId = paciente.getUsuario().getId();
        pacienteRepository.delete(paciente);
        pacienteRepository.flush();
        usuarioService.excluir(usuarioId);
    }

    private void normalizarCpf(Paciente paciente) {
        if (paciente.getCpf() != null) {
            paciente.setCpf(paciente.getCpf().replaceAll("\\D", ""));
        }
    }

    private void validarCpfDuplicado(Paciente paciente) {
        pacienteRepository.findByCpf(paciente.getCpf()).ifPresent(existente -> {
            if (paciente.getId() == null || !existente.getId().equals(paciente.getId())) {
                throw new IllegalArgumentException("Já existe um paciente cadastrado com este CPF.");
            }
        });
    }
}
