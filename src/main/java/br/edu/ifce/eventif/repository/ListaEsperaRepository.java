package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.ListaEspera;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA para fila de lista de espera de atividades esgotadas (EX01).
 */
public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {

    boolean existsByUsuarioIdAndAtividadeId(Long usuarioId, Long atividadeId);

    boolean existsByAtividadeId(Long atividadeId);

    boolean existsByAtividadeEventoId(Long eventoId);

    void deleteByUsuarioIdAndAtividadeId(Long usuarioId, Long atividadeId);

    @Query("SELECT le.atividade.id, COUNT(le) FROM ListaEspera le WHERE le.atividade.evento.id = :eventoId GROUP BY le.atividade.id")
    List<Object[]> contarFilaAgrupadaPorEventoId(@Param("eventoId") Long eventoId);

    @Query("SELECT le.atividade.id FROM ListaEspera le WHERE le.usuario.id = :usuarioId AND le.atividade.evento.id = :eventoId")
    Set<Long> findAtividadeIdsEmEsperaPorUsuarioEEvento(
        @Param("usuarioId") Long usuarioId,
        @Param("eventoId") Long eventoId
    );

    @Query("SELECT le FROM ListaEspera le JOIN FETCH le.usuario u WHERE le.atividade.id = :atividadeId ORDER BY le.dataHora ASC, le.id ASC")
    List<ListaEspera> findFilaOrdenadaPorAtividadeId(@Param("atividadeId") Long atividadeId);

    @Query("SELECT le FROM ListaEspera le JOIN FETCH le.atividade a JOIN FETCH a.evento e JOIN FETCH e.categoria WHERE le.usuario.id = :usuarioId ORDER BY le.dataHora ASC, le.id ASC")
    List<ListaEspera> findByUsuarioIdComDetalhes(@Param("usuarioId") Long usuarioId);

    @Query("SELECT le FROM ListaEspera le JOIN FETCH le.usuario u JOIN FETCH le.atividade a WHERE le.id = :id")
    Optional<ListaEspera> findByIdComDetalhes(@Param("id") Long id);
}
