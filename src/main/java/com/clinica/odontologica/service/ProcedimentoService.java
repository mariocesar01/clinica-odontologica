package com.clinica.odontologica.service;

import com.clinica.odontologica.model.Procedimento;
import com.clinica.odontologica.repository.ConsultaRepository;
import com.clinica.odontologica.repository.ProcedimentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcedimentoService {

    private final ProcedimentoRepository repository;
    private final ConsultaRepository consultaRepository;

    public ProcedimentoService(ProcedimentoRepository repository,
                               ConsultaRepository consultaRepository) {
        this.repository = repository;
        this.consultaRepository = consultaRepository;
    }

    public List<Procedimento> listarTodos() { return repository.findAll(); }

    public Procedimento buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procedimento não encontrado."));
    }

    public Procedimento salvar(Procedimento procedimento) {
        if (procedimento.getDuracaoMinutos() < 30 || procedimento.getDuracaoMinutos() > 480
                || procedimento.getDuracaoMinutos() % 30 != 0) {
            throw new IllegalArgumentException("A duração deve ser de 30 em 30 minutos, entre 30 e 480 minutos.");
        }
        return repository.save(procedimento);
    }

    public void excluir(Long id) {
        if (consultaRepository.existsByProcedimentoId(id)) {
            throw new IllegalArgumentException("Não é possível excluir um procedimento que já foi usado em consultas.");
        }
        repository.deleteById(id);
    }

    public List<Procedimento> pesquisar(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome);
    }
}
