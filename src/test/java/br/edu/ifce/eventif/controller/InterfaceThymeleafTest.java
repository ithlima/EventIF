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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InterfaceThymeleafTest {

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
    void todasAsDezTelasDevemRenderizarComFragmentsUnificadosECssDeImpressao() throws Exception {
        LocalDate hoje = LocalDate.now(clock);
        Usuario org = criarOrganizador("org.ui@eventif.edu.br");
        Usuario part = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Ana UI", "ana.ui@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Tecnologia UI"));

        Evento eventoPub = eventoService.criar(new EventoForm(
            "Evento UI Publicado", "Descricao publica", "Auditorio",
            hoje.plusDays(5), hoje.plusDays(6), cat.getId()
        ), org.getId());
        Atividade atvPub = atividadeService.criar(eventoPub.getId(), new AtividadeForm(
            "Palestra UI", TipoAtividade.PALESTRA, "Prof. UI",
            hoje.plusDays(5).atTime(9, 0),
            hoje.plusDays(5).atTime(11, 0),
            2, 40
        ), org.getId());
        eventoService.publicar(eventoPub.getId(), org.getId());
        inscricaoService.inscrever(atvPub.getId(), part.getId());

        Evento eventoEnc = eventoService.criar(new EventoForm(
            "Evento UI Encerrado", "Descricao encerrada", "Campus",
            hoje.plusDays(1), hoje.plusDays(1), cat.getId()
        ), org.getId());
        Atividade atvEnc = atividadeService.criar(eventoEnc.getId(), new AtividadeForm(
            "Minicurso UI", TipoAtividade.MINICURSO, "Profa. UI",
            hoje.plusDays(1).atTime(14, 0),
            hoje.plusDays(1).atTime(18, 0),
            4, 30
        ), org.getId());
        eventoService.publicar(eventoEnc.getId(), org.getId());
        Inscricao inscEnc = inscricaoService.inscrever(atvEnc.getId(), part.getId());
        inscricaoService.registrarPresencas(eventoEnc.getId(), atvEnc.getId(), List.of(inscEnc.getId()), org.getId());
        eventoService.encerrar(eventoEnc.getId(), org.getId());

        CustomUserDetails partPrincipal = carregarPrincipal(part.getEmail());
        CustomUserDetails orgPrincipal = carregarPrincipal(org.getEmail());
        CustomUserDetails adminPrincipal = carregarPrincipal("admin@eventif.edu.br");

        // T01, T02, T03 (publicas)
        verificarEstruturaBase(mockMvc.perform(get("/")));
        verificarEstruturaBase(mockMvc.perform(get("/eventos/" + eventoPub.getId())));
        verificarEstruturaBase(mockMvc.perform(get("/login")));
        verificarEstruturaBase(mockMvc.perform(get("/cadastro")));

        // T04 e T05 (participante + certificado com classes de impressao)
        verificarEstruturaBase(mockMvc.perform(get("/inscricoes").with(user(partPrincipal))));
        mockMvc.perform(get("/certificados/eventos/" + eventoEnc.getId()).with(user(partPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("certificado-box")))
            .andExpect(content().string(containsString("no-print")))
            .andExpect(content().string(containsString("window.print()")));

        // T06, T07, T08, T09 (organizador)
        verificarEstruturaBase(mockMvc.perform(get("/organizador/eventos").with(user(orgPrincipal))));
        verificarEstruturaBase(mockMvc.perform(get("/organizador/eventos/novo").with(user(orgPrincipal))));
        verificarEstruturaBase(mockMvc.perform(get("/organizador/eventos/" + eventoPub.getId() + "/atividades").with(user(orgPrincipal))));
        verificarEstruturaBase(mockMvc.perform(get("/organizador/eventos/" + eventoPub.getId() + "/atividades/" + atvPub.getId() + "/inscritos").with(user(orgPrincipal))));

        // T10 (admin)
        verificarEstruturaBase(mockMvc.perform(get("/admin/categorias").with(user(adminPrincipal))));
        verificarEstruturaBase(mockMvc.perform(get("/admin/usuarios").with(user(adminPrincipal))));

        // CSS proprio com regras de impressao
        mockMvc.perform(get("/css/eventif.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("@media print")))
            .andExpect(content().string(containsString(".certificado-box")));
    }

    private void verificarEstruturaBase(org.springframework.test.web.servlet.ResultActions acao) throws Exception {
        acao.andExpect(status().isOk())
            .andExpect(content().string(containsString("<header")))
            .andExpect(content().string(containsString("<nav")))
            .andExpect(content().string(containsString("<main id=\"conteudo-principal\"")))
            .andExpect(content().string(containsString("<footer")));
    }

    private Usuario criarOrganizador(String email) {
        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_ORGANIZADOR)));
        Usuario organizador = new Usuario(
            "Organizador UI",
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
