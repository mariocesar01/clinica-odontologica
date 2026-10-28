package com.clinica.odontologica.config;

import com.clinica.odontologica.service.CobrancaService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CobrancasIniciaisConfig {
    @Bean
    ApplicationRunner regularizarCobrancasExistentes(CobrancaService cobrancas) {
        return args -> cobrancas.gerarCobrancasAusentes();
    }
}
