package br.edu.ifce.eventif.controller;

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
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FluxoPublicoEParticipanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Test
    void rf01DeveValidarCamposCriarParticipanteSemPermitirEscolhaDePerfilERejeitarEmailDuplicado() throws Exception {
        mockMvc.perform(post("/cadastro")
                .param("nome", "")
                .param("email", "invalido")
                .param("senha", "123")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/cadastro"))
            .andExpect(model().attributeHasFieldErrors("cadastroForm", "nome", "email", "senha"));

        mockMvc.perform(post("/cadastro")
                .param("nome", "Beatriz Aluna")
                .param("email", "beatriz@eventif.edu.br")
                .param("senha", "Senha@123")
                .param("perfis", "ROLE_ADMIN")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        Usuario cadastrada = usuarioRepository.findWithPerfisByEmailIgnoreCase("beatriz@eventif.edu.br")
            .orElseThrow();
        assertThat(passwordEncoder.matches("Senha@123", cadastrada.getSenha())).isTrue();
        assertThat(cadastrada.getPerfis())
            .extracting(Perfil::getNome)
            .containsExactly(PerfilConstants.ROLE_PARTICIPANTE);

        mockMvc.perform(post("/cadastro")
                .param("nome", "Beatriz Outra")
                .param("email", "BEATRIZ@eventif.edu.br")
                .param("senha", "Senha@123")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/cadastro"))
            .andExpect(model().attributeHasFieldErrors("cadastroForm", "email"));
    }

    @Test
    void rf09ERf10DevemListarEventosPublicadosComPaginacaoFiltroECalcularVagasRestantes() throws Exception {
        LocalDate hoje = LocalDate.now(clock);
        Usuario org = criarOrganizador("org.pub@eventif.edu.br");
        Categoria catTech = categoriaService.criar(new CategoriaForm("Tecnologia"));
        Categoria catArte = categoriaService.criar(new CategoriaForm("Cultura"));

        Evento pubTech1 = eventoService.criar(new EventoForm(
            "Semana Java", "Evento 1", "Campus A",
            hoje.plusDays(5), hoje.plusDays(6), catTech.getId()
        ), org.getId());
        Evento pubTech2 = eventoService.criar(new EventoForm(
            "Workshop Spring", "Evento 2", "Campus B",
            hoje.plusDays(7), hoje.plusDays(8), catTech.getId()
        ), org.getId());
        Evento pubArte = eventoService.criar(new EventoForm(
            "Mostra Cultural", "Evento 3", "Patio",
            hoje.plusDays(9), hoje.plusDays(9), catArte.getId()
        ), org.getId());
        Evento rascunho = eventoService.criar(new EventoForm(
            "Evento Rascunho Oculto", "Interno", "Sala",
            hoje.plusDays(12), hoje.plusDays(12), catTech.getId()
        ), org.getId());

        Atividade atvLotada = atividadeService.criar(pubTech1.getId(), new AtividadeForm(
            "Oficina Lotada", TipoAtividade.OFICINA, "Prof. X",
            hoje.plusDays(5).atTime(9, 0),
            hoje.plusDays(5).atTime(11, 0),
            2, 1
        ), org.getId());
        atividadeService.criar(pubTech1.getId(), new AtividadeForm(
            "Palestra Aberta", TipoAtividade.PALESTRA, "Prof. Y",
            hoje.plusDays(5).atTime(14, 0),
            hoje.plusDays(5).atTime(16, 0),
            2, 10
        ), org.getId());

        eventoService.publicar(pubTech1.getId(), org.getId());
        eventoService.publicar(pubTech2.getId(), org.getId());
        eventoService.publicar(pubArte.getId(), org.getId());

        Usuario outroPart = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Ocupa Vaga", "ocupa@eventif.edu.br", "Senha@123")
        );
        inscricaoService.inscrever(atvLotada.getId(), outroPart.getId());

        mockMvc.perform(get("/").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Semana Java")))
            .andExpect(content().string(containsString("Workshop Spring")))
            .andExpect(content().string(not(containsString("Mostra Cultural"))))
            .andExpect(content().string(not(containsString("Evento Rascunho Oculto"))));

        mockMvc.perform(get("/").param("categoriaId", catArte.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Mostra Cultural")))
            .andExpect(content().string(not(containsString("Semana Java"))));

        mockMvc.perform(get("/eventos/" + rascunho.getId()))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/eventos/" + pubTech1.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Oficina Lotada")))
            .andExpect(content().string(containsString("Esgotada")))
            .andExpect(content().string(containsString("10")));
    }

    @Test
    void rf11Rf12Rf13ERs06DevemInscreverListarECancelarPeloPrincipalAutenticadoEBloquearTerceiros() throws Exception {
        LocalDate hoje = LocalDate.now(clock);
        Usuario org = criarOrganizador("org.part@eventif.edu.br");
        Usuario p1 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Participante Um", "p1.ctrl@eventif.edu.br", "Senha@123")
        );
        Usuario p2 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Participante Dois", "p2.ctrl@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Inovacao"));

        Evento evento = eventoService.criar(new EventoForm(
            "Jornada Web", "Descricao", "IFCE",
            hoje.plusDays(15), hoje.plusDays(15), cat.getId()
        ), org.getId());
        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Minicurso MVC", TipoAtividade.MINICURSO, "Prof. MVC",
            hoje.plusDays(15).atTime(8, 0),
            hoje.plusDays(15).atTime(12, 0),
            4, 20
        ), org.getId());
        eventoService.publicar(evento.getId(), org.getId());

        CustomUserDetails p1Principal = carregarPrincipal("p1.ctrl@eventif.edu.br");
        CustomUserDetails p2Principal = carregarPrincipal("p2.ctrl@eventif.edu.br");

        mockMvc.perform(post("/inscricoes/atividades/" + atividade.getId())
                .param("usuarioId", p2.getId().toString())
                .with(user(p1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + evento.getId()))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        assertThat(inscricaoService.listarMinhasInscricoes(p1.getId())).hasSize(1);
        assertThat(inscricaoService.listarMinhasInscricoes(p2.getId())).isEmpty();

        mockMvc.perform(post("/inscricoes/atividades/" + atividade.getId())
                .with(user(p1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + evento.getId()))
            .andExpect(flash().attributeExists("mensagemErro"));

        mockMvc.perform(get("/inscricoes").with(user(p1Principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Minicurso MVC")))
            .andExpect(content().string(containsString("Aguardando registro")));

        Long inscricaoP1Id = inscricaoService.listarMinhasInscricoes(p1.getId()).getFirst().inscricaoId();

        mockMvc.perform(post("/inscricoes/" + inscricaoP1Id + "/cancelar")
                .with(user(p2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/inscricoes/" + inscricaoP1Id + "/cancelar")
                .with(user(p1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/inscricoes"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        assertThat(inscricaoService.listarMinhasInscricoes(p1.getId())).isEmpty();
    }

    @Test
    void rf14ERn07DevemGerarCertificadoImprimivelSomandoPresencasENegarNaoElegivel() throws Exception {
        LocalDate hoje = LocalDate.now(clock);
        Usuario org = criarOrganizador("org.cert.ctrl@eventif.edu.br");
        Usuario pComPresenca = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Maria Presente", "maria.cert@eventif.edu.br", "Senha@123")
        );
        Usuario pSemPresenca = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Joao Ausente", "joao.cert@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Pesquisa"));

        Evento evento = eventoService.criar(new EventoForm(
            "Congresso IFCE 2026", "Descricao", "Auditorio Central",
            hoje.plusDays(1), hoje.plusDays(2), cat.getId()
        ), org.getId());
        Atividade atv1 = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Oficina JPA", TipoAtividade.OFICINA, "Prof. A",
            hoje.plusDays(1).atTime(8, 0),
            hoje.plusDays(1).atTime(12, 0),
            4, 30
        ), org.getId());
        Atividade atv2 = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Palestra Cloud", TipoAtividade.PALESTRA, "Prof. B",
            hoje.plusDays(1).atTime(14, 0),
            hoje.plusDays(1).atTime(16, 0),
            2, 30
        ), org.getId());
        eventoService.publicar(evento.getId(), org.getId());

        Inscricao inscAtv1 = inscricaoService.inscrever(atv1.getId(), pComPresenca.getId());
        inscricaoService.inscrever(atv2.getId(), pComPresenca.getId());
        inscricaoService.inscrever(atv1.getId(), pSemPresenca.getId());

        inscricaoService.registrarPresencas(evento.getId(), atv1.getId(), List.of(inscAtv1.getId()), org.getId());
        inscricaoService.registrarPresencas(evento.getId(), atv2.getId(), null, org.getId());

        CustomUserDetails pComPresencaPrincipal = carregarPrincipal("maria.cert@eventif.edu.br");
        CustomUserDetails pSemPresencaPrincipal = carregarPrincipal("joao.cert@eventif.edu.br");

        mockMvc.perform(get("/certificados/eventos/" + evento.getId()).with(user(pComPresencaPrincipal)))
            .andExpect(status().isBadRequest());

        eventoService.encerrar(evento.getId(), org.getId());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId()).with(user(pSemPresencaPrincipal)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId()).with(user(pComPresencaPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Maria Presente")))
            .andExpect(content().string(containsString("Congresso IFCE 2026")))
            .andExpect(content().string(containsString("Oficina JPA")))
            .andExpect(content().string(not(containsString("Palestra Cloud"))))
            .andExpect(content().string(containsString("4h")));
    }

    private Usuario criarOrganizador(String email) {
        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_ORGANIZADOR)));
        Usuario organizador = new Usuario(
            "Organizador Teste",
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
