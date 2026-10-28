package com.clinica.odontologica.config;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Usuario;
import com.clinica.odontologica.repository.UsuarioRepository;
import com.clinica.odontologica.service.UsuarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DadosIniciaisConfig {

    @Bean
    CommandLineRunner criarAdministradorInicial(UsuarioRepository usuarioRepository,
                                                 UsuarioService usuarioService) {
        return args -> {
            if (usuarioRepository.count() == 0) {
                Usuario admin = new Usuario();
                admin.setNome("Administrador");
                admin.setEmail("admin@clinica.com");
                admin.setSenha("admin123");
                admin.setPerfil(Perfil.ADMINISTRADOR);
                admin.setAtivo(true);
                usuarioService.salvar(admin);
            }
        };
    }
}
