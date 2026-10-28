package com.clinica.odontologica.service;

import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.Prontuario;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.ProntuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProntuarioService {

    private final ProntuarioRepository repository;
    private final ConsultaRepository consultaRepository;

    public ProntuarioService(ProntuarioRepository repository,
                             ConsultaRepository consultaRepository) {
        this.repository = repository;
        this.consultaRepository = consultaRepository;
    }

    public List<Prontuario> listarTodos() {
        return repository.findAll();
    }

    public List<Prontuario> listarPorPaciente(Long pacienteId) {
        return repository.findByConsultaPacienteIdOrderByConsultaDataHoraDesc(pacienteId);
    }

    public Prontuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prontuário não encontrado."));
    }

    public Optional<Prontuario> buscarPorConsulta(Long consultaId) {
        return repository.findByConsultaId(consultaId);
    }

    @Transactional
    public Prontuario salvar(Prontuario prontuario) {
        repository.findByConsultaId(prontuario.getConsulta().getId()).ifPresent(existente -> {
            if (prontuario.getId() == null || !existente.getId().equals(prontuario.getId())) {
                throw new IllegalArgumentException("Esta consulta já possui um prontuário cadastrado.");
            }
        });

        if (prontuario.getConsulta().getStatus() == StatusConsulta.CANCELADA)
            throw new IllegalArgumentException("Não é possível atender consulta cancelada.");
        if (prontuario.getConsulta().getDataHora().toLocalDate().isAfter(java.time.LocalDate.now()))
            throw new IllegalArgumentException("Não é possível finalizar atendimento em data futura.");
        Prontuario salvo = repository.save(prontuario);
        if (prontuario.getConsulta().getStatus() != StatusConsulta.REALIZADA) {
            prontuario.getConsulta().setFimAtendimento(java.time.LocalDateTime.now());
        }
        prontuario.getConsulta().setStatus(StatusConsulta.REALIZADA);
        consultaRepository.save(prontuario.getConsulta());
        return salvo;
    }

    public void excluir(Long id) {
        repository.deleteById(id);
    }
}
