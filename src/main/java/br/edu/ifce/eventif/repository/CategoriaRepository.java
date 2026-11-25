package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA para categorias de eventos.
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByNomeAsc();

    Optional<Categoria> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
