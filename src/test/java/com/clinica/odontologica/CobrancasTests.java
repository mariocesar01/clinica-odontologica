package com.clinica.odontologica;

import com.clinica.odontologica.enums.*;
import com.clinica.odontologica.model.*;
import com.clinica.odontologica.repository.*;
import com.clinica.odontologica.service.CobrancaService;
import com.clinica.odontologica.service.PagamentoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CobrancasTests {
    @Test void agendamentoGeraUmaUnicaCobrancaPendente() {
        PagamentoRepository repo = mock(PagamentoRepository.class);
        ConsultaRepository consultas = mock(ConsultaRepository.class);
        TentativaPagamentoOnlineRepository tentativas = mock(TentativaPagamentoOnlineRepository.class);
        CobrancaService service = new CobrancaService(repo, consultas, tentativas);
        Consulta c = new Consulta();
        c.setId(10L);
        c.setStatus(StatusConsulta.AGENDADA);
        Procedimento proc = new Procedimento();
        proc.setValor(new BigDecimal("150.00"));
        c.setProcedimento(proc);
        c.setValorProcedimento(new BigDecimal("150.00"));
        when(repo.findByConsultaId(10L)).thenReturn(Optional.empty());
        service.garantirCobranca(c);
        ArgumentCaptor<Pagamento> captor = ArgumentCaptor.forClass(Pagamento.class);
        verify(repo).save(captor.capture());
        assertEquals(StatusPagamento.PENDENTE, captor.getValue().getStatus());
        assertEquals(new BigDecimal("150.00"), captor.getValue().getValor());
        assertSame(c, captor.getValue().getConsulta());
        reset(repo);
        when(repo.findByConsultaId(10L)).thenReturn(Optional.of(captor.getValue()));
        service.garantirCobranca(c);
        verify(repo, never()).save(any(Pagamento.class));
    }

    @Test void cancelamentoCancelaCobrancaPendenteSemTentativas() {
        PagamentoRepository repo = mock(PagamentoRepository.class);
        TentativaPagamentoOnlineRepository tentativas = mock(TentativaPagamentoOnlineRepository.class);
        CobrancaService service = new CobrancaService(repo, mock(ConsultaRepository.class), tentativas);
        Pagamento p = new Pagamento(); ReflectionTestUtils.setField(p, "id", 5L); p.setStatus(StatusPagamento.PENDENTE);
        when(repo.findByConsultaId(3L)).thenReturn(Optional.of(p));
        service.cancelarCobrancaPendente(3L);
        assertEquals(StatusPagamento.CANCELADO, p.getStatus());
        verify(repo).save(p);
    }

    @Test void naoAceitaPagamentoPresencialDepoisDeTentativaOnline() {
        PagamentoRepository repo = mock(PagamentoRepository.class);
        TentativaPagamentoOnlineRepository tentativas = mock(TentativaPagamentoOnlineRepository.class);
        com.clinica.odontologica.service.PagamentoService service =
                new com.clinica.odontologica.service.PagamentoService(repo, tentativas);
        Pagamento p = new Pagamento(); ReflectionTestUtils.setField(p, "id", 9L); p.setStatus(StatusPagamento.PENDENTE);
        Consulta c = new Consulta(); c.setStatus(StatusConsulta.AGENDADA); p.setConsulta(c);
        when(repo.findById(9L)).thenReturn(Optional.of(p));
        when(tentativas.existsByPagamentoId(9L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.registrarPresencial(9L, FormaPagamento.PIX));
        assertEquals(StatusPagamento.PENDENTE, p.getStatus());
        verify(repo, never()).save(any());
    }
}
