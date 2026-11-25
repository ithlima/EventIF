package br.edu.ifce.eventif.entity;

import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.enums.TipoAtividade;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.metamodel.EntityType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class DominioJpaMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveConterExatamenteAsSeisEntidadesObrigatoriasNoMetamodel() {
        Set<String> nomesEntidades = entityManager.getEntityManager()
            .getMetamodel()
            .getEntities()
            .stream()
            .map(EntityType::getName)
            .collect(Collectors.toSet());

        assertThat(nomesEntidades).containsExactlyInAnyOrder(
            "Usuario",
            "Perfil",
            "Categoria",
            "Evento",
            "Atividade",
            "Inscricao",
            "ListaEspera"
        );
    }

    @Test
    void devePersistirGrafoCompletoComEnumsEmTextoERelacionamentos() {
        Perfil perfilOrganizador = entityManager.persist(new Perfil("ROLE_ORGANIZADOR"));
        Perfil perfilParticipante = entityManager.persist(new Perfil("ROLE_PARTICIPANTE"));

        Usuario organizador = new Usuario("Prof. Ana", "ana@eventif.edu.br", "$2a$10$hashFakeOrg", true);
        organizador.adicionarPerfil(perfilOrganizador);
        entityManager.persist(organizador);

        Usuario participante = new Usuario("Joao Aluno", "joao@eventif.edu.br", "$2a$10$hashFakePart", true);
        participante.adicionarPerfil(perfilParticipante);
        entityManager.persist(participante);

        Categoria categoria = entityManager.persist(new Categoria("Tecnologia"));

        Evento evento = new Evento(
            "Semana de Computacao",
            "Evento academico anual do IFCE.",
            "Auditorio Central",
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 3),
            StatusEvento.RASCUNHO,
            categoria,
            organizador
        );
        entityManager.persist(evento);

        Atividade atividade = new Atividade(
            "Introducao ao Spring Boot",
            TipoAtividade.MINICURSO,
            "Prof. Ana",
            LocalDateTime.of(2026, 12, 1, 14, 0),
            LocalDateTime.of(2026, 12, 1, 18, 0),
            4,
            30,
            evento
        );
        entityManager.persist(atividade);

        Inscricao inscricao = new Inscricao(
            LocalDateTime.of(2026, 11, 25, 10, 0),
            true,
            participante,
            atividade
        );
        entityManager.persist(inscricao);

        entityManager.flush();
        entityManager.clear();

        Inscricao inscricaoCarregada = entityManager.find(Inscricao.class, inscricao.getId());
        assertThat(inscricaoCarregada).isNotNull();
        assertThat(inscricaoCarregada.isPresente()).isTrue();
        assertThat(inscricaoCarregada.getUsuario().getEmail()).isEqualTo("joao@eventif.edu.br");
        assertThat(inscricaoCarregada.getAtividade().getTitulo()).isEqualTo("Introducao ao Spring Boot");
        assertThat(inscricaoCarregada.getAtividade().getEvento().getTitulo()).isEqualTo("Semana de Computacao");
        assertThat(inscricaoCarregada.getAtividade().getEvento().getCategoria().getNome()).isEqualTo("Tecnologia");
        assertThat(inscricaoCarregada.getAtividade().getEvento().getOrganizador().getEmail()).isEqualTo("ana@eventif.edu.br");

        String statusPersistido = jdbcTemplate.queryForObject(
            "SELECT status FROM eventos WHERE id = ?",
            String.class,
            evento.getId()
        );
        String tipoPersistido = jdbcTemplate.queryForObject(
            "SELECT tipo FROM atividades WHERE id = ?",
            String.class,
            atividade.getId()
        );

        assertThat(statusPersistido).isEqualTo("RASCUNHO");
        assertThat(tipoPersistido).isEqualTo("MINICURSO");
    }

    @Test
    void deveRejeitarEmailDuplicadoNoBanco() {
        entityManager.persistAndFlush(new Usuario("Usuario Um", "duplicado@eventif.edu.br", "$2a$10$hash1", true));

        Usuario segundo = new Usuario("Usuario Dois", "duplicado@eventif.edu.br", "$2a$10$hash2", true);
        assertThatThrownBy(() -> {
            entityManager.persist(segundo);
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    void deveRejeitarInscricaoDuplicadaMesmoUsuarioMesmaAtividadeEPermitirParesDistintos() {
        Usuario organizador = entityManager.persist(new Usuario("Org", "org@eventif.edu.br", "$2a$10$hashOrg", true));
        Usuario participante1 = entityManager.persist(new Usuario("Part 1", "p1@eventif.edu.br", "$2a$10$hashP1", true));
        Usuario participante2 = entityManager.persist(new Usuario("Part 2", "p2@eventif.edu.br", "$2a$10$hashP2", true));
        Categoria categoria = entityManager.persist(new Categoria("Pesquisa"));

        Evento evento = entityManager.persist(new Evento(
            "Jornada Cientifica",
            "Apresentacoes de pesquisa",
            "Campus Fortaleza",
            LocalDate.of(2026, 12, 10),
            LocalDate.of(2026, 12, 10),
            StatusEvento.PUBLICADO,
            categoria,
            organizador
        ));

        Atividade atividade1 = entityManager.persist(new Atividade(
            "Palestra de Abertura",
            TipoAtividade.PALESTRA,
            "Convidado",
            LocalDateTime.of(2026, 12, 10, 9, 0),
            LocalDateTime.of(2026, 12, 10, 11, 0),
            2,
            100,
            evento
        ));

        Atividade atividade2 = entityManager.persist(new Atividade(
            "Oficina Pratica",
            TipoAtividade.OFICINA,
            "Convidado",
            LocalDateTime.of(2026, 12, 10, 14, 0),
            LocalDateTime.of(2026, 12, 10, 16, 0),
            2,
            40,
            evento
        ));

        entityManager.persist(new Inscricao(LocalDateTime.of(2026, 11, 25, 9, 0), false, participante1, atividade1));
        entityManager.persist(new Inscricao(LocalDateTime.of(2026, 11, 25, 9, 5), false, participante2, atividade1));
        entityManager.persist(new Inscricao(LocalDateTime.of(2026, 11, 25, 9, 10), false, participante1, atividade2));
        entityManager.flush();

        Inscricao duplicada = new Inscricao(LocalDateTime.of(2026, 11, 25, 9, 15), false, participante1, atividade1);
        assertThatThrownBy(() -> {
            entityManager.persist(duplicada);
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }
}
