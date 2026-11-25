package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.Atividade;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA para atividades de eventos.
 */
public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    List<Atividade> findByEventoIdOrderByInicioAscIdAsc(Long eventoId);

    @Query("SELECT a FROM Atividade a JOIN FETCH a.evento e JOIN FETCH e.organizador WHERE a.id = :id")
    Optional<Atividade> findByIdComEventoEOrganizador(@Param("id") Long id);

    /**
     * Carrega a atividade aplicando lock pessimista de escrita ({@code SELECT ... FOR UPDATE}).
     *
     * <p>Estratégia transacional contra concorrência de vagas (ADR-07 / RN05): dentro de um método
     * {@code @Transactional} da camada {@code service}, o bloqueio serializa inscrições simultâneas
     * na mesma atividade antes da verificação de {@code Evento == PUBLICADO}, checagem de duplicidade
     * ({@code existsByUsuarioIdAndAtividadeId}), contagem de vagas ocupadas ({@code countByAtividadeId})
     * e persistência protegida pela constraint {@code uk_inscricoes_usuario_atividade}.</p>
     *
     * @param id identificador da atividade
     * @return atividade bloqueada para escrita com seu evento carregado, se existir
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Atividade a JOIN FETCH a.evento WHERE a.id = :id")
    Optional<Atividade> findByIdComLock(@Param("id") Long id);
}
