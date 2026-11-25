package br.edu.ifce.eventif.regressao;

import br.edu.ifce.eventif.dto.AtividadeForm;
import br.edu.ifce.eventif.dto.CadastroUsuarioForm;
import br.edu.ifce.eventif.dto.CategoriaForm;
import br.edu.ifce.eventif.dto.EventoForm;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.TipoAtividade;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.CategoriaRepository;
import br.edu.ifce.eventif.repository.EventoRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import br.edu.ifce.eventif.security.PerfilConstants;
import br.edu.ifce.eventif.service.AtividadeService;
import br.edu.ifce.eventif.service.CategoriaService;
import br.edu.ifce.eventif.service.EventoService;
import br.edu.ifce.eventif.service.InscricaoService;
import br.edu.ifce.eventif.service.UsuarioService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcorrenciaEArquiteturaTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private AtividadeService atividadeService;

    @Autowired
    private InscricaoService inscricaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final List<Long> atividadesCriadas = new ArrayList<>();
    private final List<Long> eventosCriados = new ArrayList<>();
    private final List<Long> categoriasCriadas = new ArrayList<>();
    private final List<Long> usuariosCriados = new ArrayList<>();

    @AfterEach
    void limparRegistrosCriados() {
        for (Long atividadeId : atividadesCriadas) {
            List<Inscricao> inscricoes = inscricaoRepository.findByAtividadeIdComUsuario(atividadeId);
            if (!inscricoes.isEmpty()) {
                inscricaoRepository.deleteAll(inscricoes);
            }
        }
        if (!atividadesCriadas.isEmpty()) {
            atividadeRepository.deleteAllById(atividadesCriadas);
            atividadesCriadas.clear();
        }
        if (!eventosCriados.isEmpty()) {
            eventoRepository.deleteAllById(eventosCriados);
            eventosCriados.clear();
        }
        if (!categoriasCriadas.isEmpty()) {
            categoriaRepository.deleteAllById(categoriasCriadas);
            categoriasCriadas.clear();
        }
        if (!usuariosCriados.isEmpty()) {
            usuarioRepository.deleteAllById(usuariosCriados);
            usuariosCriados.clear();
        }
    }

    @Test
    void rn05ConcorrenciaRealDevePermitirApenasUmaInscricaoQuandoSeisParticipantesDisputamUltimaVaga()
        throws InterruptedException {
        LocalDate hoje = LocalDate.now(clock);
        Usuario org = criarOrganizador("org.conc.vagas@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Concorrencia Vagas"));
        categoriasCriadas.add(cat.getId());

        List<Usuario> participantes = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Usuario participante = usuarioService.cadastrarParticipante(new CadastroUsuarioForm(
                "Participante Conc " + i,
                "p" + i + ".conc.vagas@eventif.edu.br",
                "Senha@123"
            ));
            usuariosCriados.add(participante.getId());
            participantes.add(participante);
        }

        Evento evento = eventoService.criar(new EventoForm(
            "Evento Concorrencia Vaga Unica", "Teste de lock pessimista", "Lab Redes",
            hoje.plusDays(10), hoje.plusDays(10), cat.getId()
        ), org.getId());
        eventosCriados.add(evento.getId());

        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Oficina Lock Pessimista", TipoAtividade.OFICINA, "Prof. Lock",
            hoje.plusDays(10).atTime(9, 0), hoje.plusDays(10).atTime(11, 0),
            2, 1
        ), org.getId());
        atividadesCriadas.add(atividade.getId());

        eventoService.publicar(evento.getId(), org.getId());

        ExecucaoConcorrente resultado = executarDisputaSimultanea(
            participantes.stream()
                .map(p -> (Runnable) () -> inscricaoService.inscrever(atividade.getId(), p.getId()))
                .toList()
        );

        assertThat(resultado.errosInesperados()).isEmpty();
        assertThat(resultado.sucessos().get()).isEqualTo(1);
        assertThat(resultado.falhasRegraNegocio().get()).isEqualTo(5);
        assertThat(inscricaoRepository.countByAtividadeId(atividade.getId())).isEqualTo(1L);
    }

    @Test
    void rn04ConcorrenciaRealDeveBloquearDuploCliqueSimultaneoDoMesmoParticipante()
        throws InterruptedException {
        LocalDate hoje = LocalDate.now(clock);
        Usuario org = criarOrganizador("org.conc.dup@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Concorrencia Duplicidade"));
        categoriasCriadas.add(cat.getId());

        Usuario participante = usuarioService.cadastrarParticipante(new CadastroUsuarioForm(
            "Participante Duplo Clique", "p.conc.dup@eventif.edu.br", "Senha@123"
        ));
        usuariosCriados.add(participante.getId());

        Evento evento = eventoService.criar(new EventoForm(
            "Evento Concorrencia Duplicidade", "Teste de idempotencia sob concorrencia", "Auditorio",
            hoje.plusDays(11), hoje.plusDays(11), cat.getId()
        ), org.getId());
        eventosCriados.add(evento.getId());

        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Palestra Duplo Clique", TipoAtividade.PALESTRA, "Prof. Concorrencia",
            hoje.plusDays(11).atTime(14, 0), hoje.plusDays(11).atTime(16, 0),
            2, 10
        ), org.getId());
        atividadesCriadas.add(atividade.getId());

        eventoService.publicar(evento.getId(), org.getId());

        List<Runnable> tarefas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tarefas.add(() -> inscricaoService.inscrever(atividade.getId(), participante.getId()));
        }

        ExecucaoConcorrente resultado = executarDisputaSimultanea(tarefas);

        assertThat(resultado.errosInesperados()).isEmpty();
        assertThat(resultado.sucessos().get()).isEqualTo(1);
        assertThat(resultado.falhasRegraNegocio().get()).isEqualTo(3);
        assertThat(inscricaoRepository.countByAtividadeId(atividade.getId())).isEqualTo(1L);
    }

    @Test
    void rt01ControllersNaoDevemAcessarRepositoriesNemReceberEntidadesJpaEmEndpointsESemEntidadeCertificado() {
        Map<String, Object> beansController = applicationContext.getBeansWithAnnotation(Controller.class);
        List<Class<?>> controllersDoProjeto = beansController.values().stream()
            .<Class<?>>map(AopUtils::getTargetClass)
            .filter(classe -> classe.getPackageName().startsWith("br.edu.ifce.eventif.controller"))
            .toList();

        assertThat(controllersDoProjeto).hasSize(7);

        for (Class<?> controllerClass : controllersDoProjeto) {
            for (Field field : controllerClass.getDeclaredFields()) {
                Class<?> tipo = field.getType();
                assertThat(tipo.getPackageName())
                    .as("Campo proibido em controller %s.%s", controllerClass.getSimpleName(), field.getName())
                    .doesNotStartWith("br.edu.ifce.eventif.repository");
                assertThat(tipo).isNotEqualTo(EntityManager.class);
                assertThat(tipo).isNotEqualTo(EntityManagerFactory.class);
                assertThat(tipo).isNotEqualTo(JdbcTemplate.class);
            }

            for (Constructor<?> constructor : controllerClass.getDeclaredConstructors()) {
                for (Parameter param : constructor.getParameters()) {
                    assertThat(param.getType().getPackageName())
                        .as("Parametro de construtor proibido em %s", controllerClass.getSimpleName())
                        .doesNotStartWith("br.edu.ifce.eventif.repository");
                    assertThat(param.getType()).isNotEqualTo(EntityManager.class);
                }
            }

            for (Method method : controllerClass.getDeclaredMethods()) {
                boolean ehEndpoint = method.isAnnotationPresent(GetMapping.class)
                    || method.isAnnotationPresent(PostMapping.class)
                    || method.isAnnotationPresent(RequestMapping.class);
                if (ehEndpoint) {
                    for (Parameter param : method.getParameters()) {
                        assertThat(param.getType().getPackageName())
                            .as("Entidade JPA exposta em endpoint %s#%s", controllerClass.getSimpleName(), method.getName())
                            .doesNotStartWith("br.edu.ifce.eventif.entity");
                    }
                }
            }
        }

        Set<String> entidades = entityManagerFactory.getMetamodel().getEntities().stream()
            .map(EntityType::getName)
            .collect(Collectors.toSet());
        assertThat(entidades).containsExactlyInAnyOrder(
            "Usuario", "Perfil", "Categoria", "Evento", "Atividade", "Inscricao", "ListaEspera"
        );
        assertThat(entidades).noneMatch(nome -> nome.toLowerCase().contains("certificado"));
    }

    private ExecucaoConcorrente executarDisputaSimultanea(List<Runnable> acoes) throws InterruptedException {
        int totalThreads = acoes.size();
        CountDownLatch prontos = new CountDownLatch(totalThreads);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch concluidos = new CountDownLatch(totalThreads);

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhasRegraNegocio = new AtomicInteger();
        List<Throwable> errosInesperados = Collections.synchronizedList(new ArrayList<>());

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);
        try {
            for (Runnable acao : acoes) {
                pool.submit(() -> {
                    prontos.countDown();
                    try {
                        largada.await(5, TimeUnit.SECONDS);
                        acao.run();
                        sucessos.incrementAndGet();
                    } catch (RegraNegocioException ex) {
                        falhasRegraNegocio.incrementAndGet();
                    } catch (Throwable ex) {
                        errosInesperados.add(ex);
                    } finally {
                        concluidos.countDown();
                    }
                });
            }

            assertThat(prontos.await(5, TimeUnit.SECONDS)).isTrue();
            largada.countDown();
            assertThat(concluidos.await(15, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        return new ExecucaoConcorrente(sucessos, falhasRegraNegocio, errosInesperados);
    }

    private Usuario criarOrganizador(String email) {
        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_ORGANIZADOR)));
        Usuario organizador = new Usuario(
            "Organizador Concorrencia",
            email,
            passwordEncoder.encode("Senha@123"),
            true
        );
        organizador.adicionarPerfil(perfilOrganizador);
        Usuario salvo = usuarioRepository.save(organizador);
        usuariosCriados.add(salvo.getId());
        return salvo;
    }

    private record ExecucaoConcorrente(
        AtomicInteger sucessos,
        AtomicInteger falhasRegraNegocio,
        List<Throwable> errosInesperados
    ) {
    }
}
