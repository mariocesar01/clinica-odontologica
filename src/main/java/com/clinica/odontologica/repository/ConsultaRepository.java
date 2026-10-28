package com.clinica.odontologica.repository;

import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    List<Consulta> findByPacienteIdOrderByDataHoraDesc(Long pacienteId);
    List<Consulta> findByDentistaIdOrderByDataHoraDesc(Long dentistaId);
    List<Consulta> findByStatusOrderByDataHoraAsc(StatusConsulta status);
    List<Consulta> findByDentistaIdAndStatusOrderByDataHoraDesc(Long dentistaId, StatusConsulta status);
    List<Consulta> findByDentistaIdAndStatusAndDataHoraBetweenOrderByDataHoraDesc(Long dentistaId,
                                                                                  StatusConsulta status,
                                                                                  LocalDateTime inicio,
                                                                                  LocalDateTime fim);
    List<Consulta> findByDentistaIdAndDataHoraBetweenOrderByDataHoraAsc(Long dentistaId,
                                                                       LocalDateTime inicio,
                                                                       LocalDateTime fim);
    List<Consulta> findByPacienteIdAndDataHoraBetweenOrderByDataHoraAsc(Long pacienteId,
                                                                        LocalDateTime inicio,
                                                                        LocalDateTime fim);
    List<com.clinica.odontologica.model.Consulta> findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
            LocalDateTime inicio, LocalDateTime fim);
    boolean existsByPacienteId(Long pacienteId);
    boolean existsByDentistaId(Long dentistaId);
    boolean existsByProcedimentoId(Long procedimentoId);
}
