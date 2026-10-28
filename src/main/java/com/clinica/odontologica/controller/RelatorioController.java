package com.clinica.odontologica.controller;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/relatorios")
public class RelatorioController {
    private final RelatorioService relatorios;
    private final SessaoService sessao;
    public RelatorioController(RelatorioService relatorios, SessaoService sessao) {
        this.relatorios = relatorios; this.sessao = sessao;
    }
    private void autorizar(HttpSession session) {
        if (!sessao.possuiPerfil(session, Perfil.ADMINISTRADOR))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente administradores.");
    }
    @GetMapping
    public RelatorioService.Resumo resumo(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim, HttpSession session) {
        autorizar(session); return relatorios.gerar(inicio, fim);
    }
    @GetMapping(value="/csv", produces="text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> csv(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
             @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim, HttpSession session) {
        autorizar(session);
        byte[] dados = relatorios.exportarCsv(relatorios.gerar(inicio,fim)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=relatorio-clinica-" + inicio + "-" + fim + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(dados);
    }
}
