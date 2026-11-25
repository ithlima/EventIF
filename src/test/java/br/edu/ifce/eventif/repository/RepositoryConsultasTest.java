package br.edu.ifce.eventif.repository;

import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.enums.TipoAtividade;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryConsultasTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Test
    void deveLocalizarPerfilPorNomeEUsuarioPorEmailEVerificarExistencia() {
        Perfil perfil = perfilRepository.save(new Perfil("ROLE_PARTICIPANTE"));
        Usuario usuario = new Usuario("Maria Silva", "maria@eventif.edu.br", "$2a$10$hashMaria", true);
        usuario.adicionarPerfil(perfil);
        usuarioRepository.save(usuario);

        entityManager.flush();
        entityManager.clear();

        assertThat(perfilRepository.findByNome("ROLE_PARTICIPANTE"))
            .isPresent()
            .get()
            .extracting(Perfil::getNome)
            .isEqualTo("ROLE_PARTICIPANTE");

        assertThat(usuarioRepository.findByEmail("maria@eventif.edu.br")).isPresent();
        assertThat(usuarioRepository.existsByEmailIgnoreCase("MARIA@eventif.edu.br")).isTrue();
        assertThat(usuarioRepository.existsByEmailIgnoreCase("outro@eventif.edu.br")).isFalse();

        Usuario carregadoComPerfis = usuarioRepository
            .findWithPerfisByEmailIgnoreCase("MARIA@eventif.edu.br")
            .orElseThrow();
        assertThat(carregadoComPerfis.getPerfis())
            .extracting(Perfil::getNome)
            .containsExactly("ROLE_PARTICIPANTE");
    }

    @Test
    void deveBuscarEventosPublicadosComPaginacaoEFiltroOpcionalPorCategoriaEListarPorOrganizador() {
        Usuario org1 = usuarioRepository.save(new Usuario("Org 1", "org1@eventif.edu.br", "$2a$10$h1", true));
        Usuario org2 = usuarioRepository.save(new Usuario("Org 2", "org2@eventif.edu.br", "$2a$10$h2", true));

        Categoria catTech = categoriaRepository.save(new Categoria("Tecnologia"));
        Categoria catExt = categoriaRepository.save(new Categoria("Extensao"));

        Evento pub1 = eventoRepository.save(new Evento(
            "Semana de Java",
            "Evento Java",
            "Bloco A",
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 2),
            StatusEvento.PUBLICADO,
            catTech,
            org1
        ));
        Evento pub2 = eventoRepository.save(new Evento(
            "Mostra de Extensao",
            "Projetos sociais",
            "Patio",
            LocalDate.of(2026, 12, 5),
            LocalDate.of(2026, 12, 6),
            StatusEvento.PUBLICADO,
            catExt,
            org1
        ));
        eventoRepository.save(new Evento(
            "Rascunho Interno",
            "Nao publicado",
            "Sala 1",
            LocalDate.of(2026, 12, 7),
            LocalDate.of(2026, 12, 7),
            StatusEvento.RASCUNHO,
            catTech,
            org1
        ));
        eventoRepository.save(new Evento(
            "Evento Encerrado",
            "Ja finalizado",
            "Auditorio",
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 11),
            StatusEvento.ENCERRADO,
            catTech,
            org2
        ));

        entityManager.flush();
        entityManager.clear();

        Page<Evento> pagina1 = eventoRepository.findByStatus(
            StatusEvento.PUBLICADO,
            PageRequest.of(0, 1, Sort.by("dataInicio").ascending())
        );
        assertThat(pagina1.getTotalElements()).isEqualTo(2);
        assertThat(pagina1.getTotalPages()).isEqualTo(2);
        assertThat(pagina1.getContent()).hasSize(1);
        assertThat(pagina1.getContent().getFirst().getId()).isEqualTo(pub1.getId());
        assertThat(pagina1.getContent().getFirst().getCategoria().getNome()).isEqualTo("Tecnologia");

        Page<Evento> todosPublicados = eventoRepository.buscarPorStatusECategoriaOpcional(
            StatusEvento.PUBLICADO,
            null,
            PageRequest.of(0, 10, Sort.by("dataInicio").ascending())
        );
        assertThat(todosPublicados.getContent())
            .extracting(Evento::getId)
            .containsExactly(pub1.getId(), pub2.getId());

        Page<Evento> filtradosPorCategoria = eventoRepository.findByStatusAndCategoriaId(
            StatusEvento.PUBLICADO,
            catExt.getId(),
            PageRequest.of(0, 10)
        );
        assertThat(filtradosPorCategoria.getTotalElements()).isEqualTo(1);
        assertThat(filtradosPorCategoria.getContent().getFirst().getTitulo()).isEqualTo("Mostra de Extensao");

        List<Evento> eventosOrg1 = eventoRepository.findByOrganizadorIdOrderByDataInicioDescIdDesc(org1.getId());
        assertThat(eventosOrg1).hasSize(3);
        assertThat(eventosOrg1).allSatisfy(e -> assertThat(e.getCategoria().getNome()).isNotBlank());

        List<Evento> eventosOrg2 = eventoRepository.findByOrganizadorIdOrderByDataInicioDescIdDesc(org2.getId());
        assertThat(eventosOrg2).hasSize(1);
        assertThat(eventosOrg2.getFirst().getTitulo()).isEqualTo("Evento Encerrado");
    }

    @Test
    void deveListarAtividadesDoEventoOrdenadasECarregarAtividadeComLockPessimista() {
        Usuario org = usuarioRepository.save(new Usuario("Org", "org@eventif.edu.br", "$2a$10$h", true));
        Categoria cat = categoriaRepository.save(new Categoria("Computacao"));
        Evento evento = eventoRepository.save(new Evento(
            "Congresso IFCE",
            "Descricao",
            "Fortaleza",
            LocalDate.of(2026, 12, 1),
            LocalDate.of(2026, 12, 2),
            StatusEvento.PUBLICADO,
            cat,
            org
        ));

        Atividade tarde = atividadeRepository.save(new Atividade(
            "Oficina Tarde",
            TipoAtividade.OFICINA,
            "Ministrante B",
            LocalDateTime.of(2026, 12, 1, 14, 0),
            LocalDateTime.of(2026, 12, 1, 17, 0),
            3,
            25,
            evento
        ));
        Atividade manha = atividadeRepository.save(new Atividade(
            "Palestra Manha",
            TipoAtividade.PALESTRA,
            "Ministrante A",
            LocalDateTime.of(2026, 12, 1, 9, 0),
            LocalDateTime.of(2026, 12, 1, 11, 0),
            2,
            80,
            evento
        ));

        entityManager.flush();
        entityManager.clear();

        List<Atividade> atividades = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(evento.getId());
        assertThat(atividades)
            .extracting(Atividade::getId)
            .containsExactly(manha.getId(), tarde.getId());

        Atividade bloqueada = atividadeRepository.findByIdComLock(manha.getId()).orElseThrow();
        assertThat(bloqueada.getTitulo()).isEqualTo("Palestra Manha");
        assertThat(bloqueada.getEvento().getStatus()).isEqualTo(StatusEvento.PUBLICADO);
    }

    @Test
    void deveExecutarConsultasDeInscricaoContagemDeVagasListaDoParticipanteListaDeInscritosEPresenca() {
        Usuario org = usuarioRepository.save(new Usuario("Org", "org@eventif.edu.br", "$2a$10$h", true));
        Usuario part1 = usuarioRepository.save(new Usuario("Bruno", "bruno@eventif.edu.br", "$2a$10$h1", true));
        Usuario part2 = usuarioRepository.save(new Usuario("Ana", "ana@eventif.edu.br", "$2a$10$h2", true));
        Categoria cat = categoriaRepository.save(new Categoria("Inovacao"));

        Evento evento = eventoRepository.save(new Evento(
            "Simposio Tech",
            "Descricao",
            "Campus",
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 21),
            StatusEvento.ENCERRADO,
            cat,
            org
        ));

        Atividade atv1 = atividadeRepository.save(new Atividade(
            "Minicurso Spring",
            TipoAtividade.MINICURSO,
            "Prof. Carlos",
            LocalDateTime.of(2026, 11, 20, 8, 0),
            LocalDateTime.of(2026, 11, 20, 12, 0),
            4,
            20,
            evento
        ));
        Atividade atv2 = atividadeRepository.save(new Atividade(
            "Palestra Arquitetura",
            TipoAtividade.PALESTRA,
            "Profa. Bia",
            LocalDateTime.of(2026, 11, 20, 14, 0),
            LocalDateTime.of(2026, 11, 20, 16, 0),
            2,
            50,
            evento
        ));

        Inscricao insc1 = inscricaoRepository.save(
            new Inscricao(LocalDateTime.of(2026, 11, 15, 10, 0), true, part1, atv1)
        );
        inscricaoRepository.save(
            new Inscricao(LocalDateTime.of(2026, 11, 15, 10, 5), false, part1, atv2)
        );
        inscricaoRepository.save(
            new Inscricao(LocalDateTime.of(2026, 11, 15, 10, 10), true, part2, atv1)
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(part1.getId(), atv1.getId())).isTrue();
        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(part2.getId(), atv2.getId())).isFalse();
        assertThat(inscricaoRepository.countByAtividadeId(atv1.getId())).isEqualTo(2L);
        assertThat(inscricaoRepository.countByAtividadeId(atv2.getId())).isEqualTo(1L);

        Map<Long, Long> contagemPorAtividade = inscricaoRepository
            .contarInscricoesAgrupadasPorEventoId(evento.getId())
            .stream()
            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        assertThat(contagemPorAtividade)
            .containsEntry(atv1.getId(), 2L)
            .containsEntry(atv2.getId(), 1L);

        List<Inscricao> inscricoesPart1 = inscricaoRepository.findByUsuarioIdComDetalhes(part1.getId());
        assertThat(inscricoesPart1).hasSize(2);
        assertThat(inscricoesPart1.getFirst().getAtividade().getEvento().getCategoria().getNome())
            .isEqualTo("Inovacao");

        List<Inscricao> inscritosAtv1 = inscricaoRepository.findByAtividadeIdComUsuario(atv1.getId());
        assertThat(inscritosAtv1)
            .extracting(i -> i.getUsuario().getNome())
            .containsExactly("Ana", "Bruno");

        assertThat(inscricaoRepository.findByIdAndUsuarioIdComAtividade(insc1.getId(), part1.getId())).isPresent();
        assertThat(inscricaoRepository.findByIdAndUsuarioIdComAtividade(insc1.getId(), part2.getId())).isEmpty();

        List<Inscricao> presentesPart1 = inscricaoRepository.findPresentesPorUsuarioIdEEventoId(part1.getId(), evento.getId());
        assertThat(presentesPart1).hasSize(1);
        assertThat(presentesPart1.getFirst().getAtividade().getCargaHoraria()).isEqualByComparingTo("4");
    }
}
