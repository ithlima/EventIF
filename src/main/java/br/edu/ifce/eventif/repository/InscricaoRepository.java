package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.Inscricao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA para inscrições e registros de presença.
 */
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    boolean existsByUsuarioIdAndAtividadeId(Long usuarioId, Long atividadeId);

    long countByAtividadeId(Long atividadeId);

    @Query("SELECT i.atividade.id, COUNT(i) FROM Inscricao i WHERE i.atividade.evento.id = :eventoId GROUP BY i.atividade.id")
    List<Object[]> contarInscricoesAgrupadasPorEventoId(@Param("eventoId") Long eventoId);

    @Query("SELECT i FROM Inscricao i JOIN FETCH i.atividade a JOIN FETCH a.evento e JOIN FETCH e.categoria WHERE i.usuario.id = :usuarioId ORDER BY a.inicio DESC, i.id DESC")
    List<Inscricao> findByUsuarioIdComDetalhes(@Param("usuarioId") Long usuarioId);

    @Query("SELECT i.atividade.id FROM Inscricao i WHERE i.usuario.id = :usuarioId AND i.atividade.evento.id = :eventoId")
    Set<Long> findAtividadeIdsInscritasPorUsuarioEEvento(
        @Param("usuarioId") Long usuarioId,
        @Param("eventoId") Long eventoId
    );

    @Query("SELECT i FROM Inscricao i JOIN FETCH i.usuario u WHERE i.atividade.id = :atividadeId ORDER BY u.nome ASC, i.id ASC")
    List<Inscricao> findByAtividadeIdComUsuario(@Param("atividadeId") Long atividadeId);

    @Query("SELECT i FROM Inscricao i JOIN FETCH i.usuario u JOIN FETCH i.atividade a JOIN FETCH a.evento e WHERE i.id = :id")
    Optional<Inscricao> findByIdComDetalhes(@Param("id") Long id);

    @Query("SELECT i FROM Inscricao i JOIN FETCH i.atividade a JOIN FETCH a.evento e WHERE i.id = :id AND i.usuario.id = :usuarioId")
    Optional<Inscricao> findByIdAndUsuarioIdComAtividade(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("SELECT i FROM Inscricao i JOIN FETCH i.atividade a JOIN FETCH a.evento e WHERE i.usuario.id = :usuarioId AND e.id = :eventoId AND i.presente = true ORDER BY a.inicio ASC")
    List<Inscricao> findPresentesPorUsuarioIdEEventoId(
        @Param("usuarioId") Long usuarioId,
        @Param("eventoId") Long eventoId
    );

    boolean existsByAtividadeId(Long atividadeId);

    boolean existsByAtividadeEventoId(Long eventoId);

    @Query("SELECT i FROM Inscricao i JOIN FETCH i.atividade a WHERE i.usuario.id = :usuarioId AND a.inicio < :fim AND :inicio < a.fim ORDER BY a.inicio ASC")
    List<Inscricao> findConflitosDeHorario(
        @Param("usuarioId") Long usuarioId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );
}
