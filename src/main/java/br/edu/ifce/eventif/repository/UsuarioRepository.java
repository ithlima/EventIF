package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA para usuários do sistema.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "perfis")
    Optional<Usuario> findWithPerfisByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "perfis")
    List<Usuario> findAllByOrderByNomeAsc();

    @EntityGraph(attributePaths = "perfis")
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findByIdComPerfis(@Param("id") Long id);
}
