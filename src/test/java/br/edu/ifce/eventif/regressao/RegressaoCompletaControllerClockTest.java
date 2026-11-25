package br.edu.ifce.eventif.regressao;

import br.edu.ifce.eventif.dto.AtividadeForm;
import br.edu.ifce.eventif.dto.CadastroUsuarioForm;
import br.edu.ifce.eventif.dto.CategoriaForm;
import br.edu.ifce.eventif.dto.EventoForm;
import br.edu.ifce.eventif.dto.MinhaInscricaoView;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.TipoAtividade;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import br.edu.ifce.eventif.security.CustomUserDetails;
import br.edu.ifce.eventif.security.PerfilConstants;
import br.edu.ifce.eventif.service.AtividadeService;
import br.edu.ifce.eventif.service.CategoriaService;
import br.edu.ifce.eventif.service.EventoService;
import br.edu.ifce.eventif.service.InscricaoService;
import br.edu.ifce.eventif.service.UsuarioService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegressaoCompletaControllerClockTest {

    @TestConfiguration
    static class ClockTestConfig {

        @Bean
        @Primary
        ControllableClock controllableClock() {
            return new ControllableClock();
        }
    }

    static class ControllableClock extends Clock {

        private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
        private static final Instant INSTANTE_PADRAO =
            LocalDateTime.of(2026, 11, 25, 10, 0).atZone(ZONE).toInstant();

        private final AtomicReference<Instant> instante = new AtomicReference<>(INSTANTE_PADRAO);

        void definirDataHora(LocalDateTime dataHora) {
            instante.set(dataHora.atZone(ZONE).toInstant());
        }

        void resetar() {
            instante.set(INSTANTE_PADRAO);
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return instante.get();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ControllableClock controllableClock;

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
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void restaurarRelogio() {
        controllableClock.resetar();
    }

    @Test
    void cenario3HttpDeveBloquearInscricaoQuandoVagasEsgotadasMantendoLimite() throws Exception {
        Usuario org = criarOrganizador("org.reg.vagas@eventif.edu.br");
        Usuario p1 = criarParticipante("Part Vaga 1", "p1.reg.vagas@eventif.edu.br");
        Usuario p2 = criarParticipante("Part Vaga 2", "p2.reg.vagas@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Regressao Vagas"));

        Evento evento = eventoService.criar(new EventoForm(
            "Evento Regressao Vagas", "Descricao", "Auditório",
            LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10), cat.getId()
        ), org.getId());
        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Oficina Vaga Unica", TipoAtividade.OFICINA, "Prof. Vagas",
            LocalDateTime.of(2026, 12, 10, 9, 0),
            LocalDateTime.of(2026, 12, 10, 11, 0),
            2, 1
        ), org.getId());
        eventoService.publicar(evento.getId(), org.getId());

        CustomUserDetails p1Principal = carregarPrincipal(p1.getEmail());
        CustomUserDetails p2Principal = carregarPrincipal(p2.getEmail());

        mockMvc.perform(post("/inscricoes/atividades/" + atividade.getId())
                .with(user(p1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + evento.getId()))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        mockMvc.perform(post("/inscricoes/atividades/" + atividade.getId())
                .with(user(p2Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + evento.getId()))
            .andExpect(flash().attribute("mensagemErro", containsString("vagas")));

        assertThat(inscricaoRepository.countByAtividadeId(atividade.getId())).isEqualTo(1L);
    }

    @Test
    void cenario4HttpDevePermitirCancelamentoAntesDoInicioEBloquearNoInicioEAposInicio() throws Exception {
        Usuario org = criarOrganizador("org.reg.cancel@eventif.edu.br");
        Usuario part = criarParticipante("Part Cancelamento", "p.reg.cancel@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Regressao Cancelamento"));

        Evento evento = eventoService.criar(new EventoForm(
            "Evento Regressao Horario", "Descricao", "Lab 02",
            LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 5), cat.getId()
        ), org.getId());
        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Minicurso Horario Controlado", TipoAtividade.MINICURSO, "Prof. Clock",
            LocalDateTime.of(2026, 12, 5, 14, 0),
            LocalDateTime.of(2026, 12, 5, 16, 0),
            2, 20
        ), org.getId());
        eventoService.publicar(evento.getId(), org.getId());

        CustomUserDetails partPrincipal = carregarPrincipal(part.getEmail());

        controllableClock.definirDataHora(LocalDateTime.of(2026, 12, 5, 13, 59));
        Inscricao inscricao1 = inscricaoService.inscrever(atividade.getId(), part.getId());
        List<MinhaInscricaoView> antes = inscricaoService.listarMinhasInscricoes(part.getId());
        assertThat(antes).hasSize(1);
        assertThat(antes.getFirst().cancelavel()).isTrue();

        mockMvc.perform(post("/inscricoes/" + inscricao1.getId() + "/cancelar")
                .with(user(partPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/inscricoes"))
            .andExpect(flash().attributeExists("mensagemSucesso"));
        assertThat(inscricaoRepository.existsById(inscricao1.getId())).isFalse();

        Inscricao inscricao2 = inscricaoService.inscrever(atividade.getId(), part.getId());

        controllableClock.definirDataHora(LocalDateTime.of(2026, 12, 5, 14, 0));
        mockMvc.perform(post("/inscricoes/" + inscricao2.getId() + "/cancelar")
                .with(user(partPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/inscricoes"))
            .andExpect(flash().attribute("mensagemErro", containsString("antes do início")));
        assertThat(inscricaoRepository.existsById(inscricao2.getId())).isTrue();

        controllableClock.definirDataHora(LocalDateTime.of(2026, 12, 5, 15, 0));
        List<MinhaInscricaoView> durante = inscricaoService.listarMinhasInscricoes(part.getId());
        assertThat(durante).hasSize(1);
        assertThat(durante.getFirst().cancelavel()).isFalse();

        mockMvc.perform(post("/inscricoes/" + inscricao2.getId() + "/cancelar")
                .with(user(partPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/inscricoes"))
            .andExpect(flash().attribute("mensagemErro", containsString("antes do início")));
        assertThat(inscricaoRepository.existsById(inscricao2.getId())).isTrue();
    }

    @Test
    void cenarios5E6HttpDevemIsolarInscricoesECertificadosPorSessaoEBloquearNaoElegiveis() throws Exception {
        Usuario org = criarOrganizador("org.reg.cert@eventif.edu.br");
        Usuario p1 = criarParticipante("Maria Regressao", "p1.reg.cert@eventif.edu.br");
        Usuario p2 = criarParticipante("Carlos Ausente", "p2.reg.cert@eventif.edu.br");
        Usuario p3 = criarParticipante("Joana SemInscricao", "p3.reg.cert@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Regressao Certificado"));

        Evento evento = eventoService.criar(new EventoForm(
            "Jornada Regressao Certificados", "Descricao", "Campus Fortaleza",
            LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 8), cat.getId()
        ), org.getId());
        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Palestra Arquitetura Web", TipoAtividade.PALESTRA, "Prof. Cert",
            LocalDateTime.of(2026, 12, 8, 9, 0),
            LocalDateTime.of(2026, 12, 8, 12, 0),
            3, 30
        ), org.getId());

        CustomUserDetails p1Principal = carregarPrincipal(p1.getEmail());
        CustomUserDetails p2Principal = carregarPrincipal(p2.getEmail());
        CustomUserDetails p3Principal = carregarPrincipal(p3.getEmail());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId()).with(user(p1Principal)))
            .andExpect(status().isBadRequest());

        eventoService.publicar(evento.getId(), org.getId());
        Inscricao inscP1 = inscricaoService.inscrever(atividade.getId(), p1.getId());
        inscricaoService.inscrever(atividade.getId(), p2.getId());
        inscricaoService.registrarPresencas(evento.getId(), atividade.getId(), List.of(inscP1.getId()), org.getId());

        mockMvc.perform(get("/inscricoes").with(user(p3Principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Palestra Arquitetura Web"))));

        mockMvc.perform(get("/certificados/eventos/" + evento.getId()).with(user(p1Principal)))
            .andExpect(status().isBadRequest());

        eventoService.encerrar(evento.getId(), org.getId());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId()).with(user(p2Principal)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId())
                .param("usuarioId", p1.getId().toString())
                .with(user(p3Principal)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId()).with(user(p1Principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Maria Regressao")))
            .andExpect(content().string(not(containsString("Carlos Ausente"))));
    }

    @Test
    void cenario7DeveProtegerCicloDeVidaEIntegridadeDeEventosEAtividades() {
        Usuario org = criarOrganizador("org.reg.ciclo@eventif.edu.br");
        Usuario p1 = criarParticipante("Part Ciclo 1", "p1.reg.ciclo@eventif.edu.br");
        Usuario p2 = criarParticipante("Part Ciclo 2", "p2.reg.ciclo@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Regressao Ciclo"));

        Evento evento = eventoService.criar(new EventoForm(
            "Evento Ciclo Vida", "Descricao", "Bloco B",
            LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 12), cat.getId()
        ), org.getId());
        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Oficina Ciclo", TipoAtividade.OFICINA, "Prof. Ciclo",
            LocalDateTime.of(2026, 12, 12, 10, 0),
            LocalDateTime.of(2026, 12, 12, 12, 0),
            2, 5
        ), org.getId());

        assertThatThrownBy(() -> eventoService.encerrar(evento.getId(), org.getId()))
            .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> inscricaoService.registrarPresencas(
            evento.getId(), atividade.getId(), List.of(), org.getId()
        )).isInstanceOf(RegraNegocioException.class);

        eventoService.publicar(evento.getId(), org.getId());

        assertThatThrownBy(() -> eventoService.publicar(evento.getId(), org.getId()))
            .isInstanceOf(RegraNegocioException.class);

        inscricaoService.inscrever(atividade.getId(), p1.getId());
        inscricaoService.inscrever(atividade.getId(), p2.getId());

        assertThatThrownBy(() -> atividadeService.atualizar(
            evento.getId(),
            atividade.getId(),
            new AtividadeForm(
                "Oficina Ciclo", TipoAtividade.OFICINA, "Prof. Ciclo",
                LocalDateTime.of(2026, 12, 12, 10, 0),
                LocalDateTime.of(2026, 12, 12, 12, 0),
                2, 1
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("inscritos");

        assertThatThrownBy(() -> atividadeService.excluir(evento.getId(), atividade.getId(), org.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("inscritos");

        assertThatThrownBy(() -> eventoService.excluir(evento.getId(), org.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("inscritos");

        eventoService.encerrar(evento.getId(), org.getId());

        assertThatThrownBy(() -> eventoService.atualizar(
            evento.getId(),
            new EventoForm(
                "Evento Encerrado Editado", "Desc", "Bloco B",
                LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 12), cat.getId()
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class);

        assertThatThrownBy(() -> eventoService.excluir(evento.getId(), org.getId()))
            .isInstanceOf(RegraNegocioException.class);

        assertThatThrownBy(() -> atividadeService.criar(
            evento.getId(),
            new AtividadeForm(
                "Nova Atividade Encerrado", TipoAtividade.PALESTRA, "Prof",
                LocalDateTime.of(2026, 12, 12, 14, 0),
                LocalDateTime.of(2026, 12, 12, 16, 0),
                2, 10
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class);

        assertThatThrownBy(() -> atividadeService.atualizar(
            evento.getId(),
            atividade.getId(),
            new AtividadeForm(
                "Oficina Encerrado Editada", TipoAtividade.OFICINA, "Prof. Ciclo",
                LocalDateTime.of(2026, 12, 12, 10, 0),
                LocalDateTime.of(2026, 12, 12, 12, 0),
                2, 5
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class);

        assertThatThrownBy(() -> atividadeService.excluir(evento.getId(), atividade.getId(), org.getId()))
            .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cenario8TodasAsRotasAdminDevemBloquearParticipanteOrganizadorEAnonimo() throws Exception {
        Usuario participante = criarParticipante("Part Admin Check", "p.reg.adm@eventif.edu.br");
        Usuario organizador = criarOrganizador("org.reg.adm@eventif.edu.br");

        CustomUserDetails partPrincipal = carregarPrincipal(participante.getEmail());
        CustomUserDetails orgPrincipal = carregarPrincipal(organizador.getEmail());

        List<String> rotasGet = List.of("/admin/categorias", "/admin/categorias/1/editar", "/admin/usuarios");
        List<String> rotasPost = List.of(
            "/admin/categorias",
            "/admin/categorias/1",
            "/admin/categorias/1/excluir",
            "/admin/usuarios/1/conceder-organizador",
            "/admin/usuarios/1/remover-organizador"
        );

        for (String rota : rotasGet) {
            mockMvc.perform(get(rota).with(user(partPrincipal))).andExpect(status().isForbidden());
            mockMvc.perform(get(rota).with(user(orgPrincipal))).andExpect(status().isForbidden());
            mockMvc.perform(get(rota))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        }

        for (String rota : rotasPost) {
            mockMvc.perform(post(rota).with(user(partPrincipal)).with(csrf())).andExpect(status().isForbidden());
            mockMvc.perform(post(rota).with(user(orgPrincipal)).with(csrf())).andExpect(status().isForbidden());
            mockMvc.perform(post(rota).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    private Usuario criarParticipante(String nome, String email) {
        return usuarioService.cadastrarParticipante(new CadastroUsuarioForm(nome, email, "Senha@123"));
    }

    private Usuario criarOrganizador(String email) {
        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_ORGANIZADOR)));
        Usuario organizador = new Usuario(
            "Organizador Regressao",
            email,
            passwordEncoder.encode("Senha@123"),
            true
        );
        organizador.adicionarPerfil(perfilOrganizador);
        return usuarioRepository.save(organizador);
    }

    private CustomUserDetails carregarPrincipal(String email) {
        Usuario usuario = usuarioRepository.findWithPerfisByEmailIgnoreCase(email).orElseThrow();
        return CustomUserDetails.fromUsuario(usuario);
    }
}
