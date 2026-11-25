package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.Perfil;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA para perfis de acesso do sistema.
 */
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByNome(String nome);

    boolean existsByNome(String nome);
}
