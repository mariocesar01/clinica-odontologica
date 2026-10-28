package com.clinica.odontologica.repository;

import com.clinica.odontologica.enums.Perfil;
import com.clinica.odontologica.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findByPerfilIn(List<Perfil> perfis);
}
