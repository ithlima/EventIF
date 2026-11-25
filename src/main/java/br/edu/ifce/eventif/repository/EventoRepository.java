package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.enums.StatusEvento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA para eventos acadêmicos, incluindo consultas derivadas e paginadas (RT02).
 */
public interface EventoRepository extends JpaRepository<Evento, Long> {

    @EntityGraph(attributePaths = {"categoria", "organizador"})
    Page<Evento> findByStatus(StatusEvento status, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "organizador"})
    Page<Evento> findByStatusAndCategoriaId(StatusEvento status, Long categoriaId, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "organizador"})
    @Query("SELECT e FROM Evento e WHERE e.status = :status AND (:categoriaId IS NULL OR e.categoria.id = :categoriaId)")
    Page<Evento> buscarPorStatusECategoriaOpcional(
        @Param("status") StatusEvento status,
        @Param("categoriaId") Long categoriaId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "categoria")
    List<Evento> findByOrganizadorIdOrderByDataInicioDescIdDesc(Long organizadorId);

    @EntityGraph(attributePaths = {"categoria", "organizador"})
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> findByIdComDetalhes(@Param("id") Long id);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByTituloIgnoreCase(String titulo);

    Optional<Evento> findByTituloIgnoreCase(String titulo);
}
