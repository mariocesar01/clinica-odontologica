package com.clinica.odontologica;

import com.clinica.odontologica.enums.StatusConsulta;
import com.clinica.odontologica.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegrasNegocioTests {
    @Test void progressoRepresentaEtapas() {
        FluxoAtendimentoService fluxo = new FluxoAtendimentoService(null, null);
        assertEquals(0, fluxo.percentual(StatusConsulta.AGENDADA));
        assertEquals(20, fluxo.percentual(StatusConsulta.CONFIRMADA));
        assertEquals(40, fluxo.percentual(StatusConsulta.AGUARDANDO));
        assertEquals(70, fluxo.percentual(StatusConsulta.EM_ATENDIMENTO));
        assertEquals(100, fluxo.percentual(StatusConsulta.REALIZADA));
        assertEquals(0, fluxo.percentual(StatusConsulta.CANCELADA));
    }
    @Test void assinaturaWebhookExigeHmacCorreto() throws Exception {
        AssinaturaWebhookService assinatura = new AssinaturaWebhookService();
        ReflectionTestUtils.setField(assinatura, "segredo", "segredo-local-apenas-para-testes");
        String ts = Long.toString(Instant.now().getEpochSecond());
        String msg = "id:12345;request-id:request-test;ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("segredo-local-apenas-para-testes".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hex = HexFormat.of().formatHex(mac.doFinal(msg.getBytes(StandardCharsets.UTF_8)));
        assertTrue(assinatura.assinaturaValida("ts=" + ts + ",v1=" + hex, "request-test", "12345"));
        assertFalse(assinatura.assinaturaValida("ts=" + ts + ",v1=" + hex, "request-invalida", "12345"));
        assertFalse(assinatura.assinaturaValida("ts=" + ts + ",v1=" + hex, "request-test", "54321"));
        assertFalse(assinatura.assinaturaValida(null, "request-test", "12345"));
    }
    @Test void csvProtegeDeFormulasEmNomes() {
        RelatorioService service = new RelatorioService(null, null, null);
        RelatorioService.Resumo resumo = new RelatorioService.Resumo(
                LocalDate.of(2026,10,1), LocalDate.of(2026,10,31),
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                0, 0, 0, 0, 0, 0,
                List.of(new RelatorioService.Quantidade("=IMPORTXML()", 1)), List.of());
        String csv = service.exportarCsv(resumo);
        assertTrue(csv.contains("'=IMPORTXML()"));
        assertTrue(csv.contains("Consultas realizadas"));
    }
}
