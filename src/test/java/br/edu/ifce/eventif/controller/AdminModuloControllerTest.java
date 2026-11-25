package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.dto.CadastroUsuarioForm;
import br.edu.ifce.eventif.dto.CategoriaForm;
import br.edu.ifce.eventif.dto.EventoForm;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.repository.CategoriaRepository;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import br.edu.ifce.eventif.security.CustomUserDetails;
import br.edu.ifce.eventif.security.PerfilConstants;
import br.edu.ifce.eventif.service.CategoriaService;
import br.edu.ifce.eventif.service.EventoService;
import br.edu.ifce.eventif.service.UsuarioService;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminModuloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Test
    void rf03DeveExecutarCrudDeCategoriaValidandoDuplicidadeEVinculos() throws Exception {
        CustomUserDetails adminPrincipal = carregarPrincipal("admin@eventif.edu.br");
        Usuario org = criarOrganizador("org.cat@eventif.edu.br");

        mockMvc.perform(post("/admin/categorias")
                .param("nome", "")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/categorias"))
            .andExpect(model().attributeHasFieldErrors("categoriaForm", "nome"));

        mockMvc.perform(post("/admin/categorias")
                .param("nome", "Inovacao")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/categorias"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        Categoria criada = categoriaRepository.findByNomeIgnoreCase("Inovacao").orElseThrow();

        mockMvc.perform(post("/admin/categorias")
                .param("nome", "inovacao")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/categorias"))
            .andExpect(model().attributeHasFieldErrors("categoriaForm", "nome"));

        mockMvc.perform(get("/admin/categorias/" + criada.getId() + "/editar").with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Editar Categoria")))
            .andExpect(content().string(containsString("Inovacao")));

        mockMvc.perform(post("/admin/categorias/" + criada.getId())
                .param("nome", "Inovacao Tecnologica")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/categorias"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        assertThat(categoriaRepository.findById(criada.getId()).orElseThrow().getNome())
            .isEqualTo("Inovacao Tecnologica");

        LocalDate hoje = LocalDate.now(clock);
        Categoria vinculada = categoriaService.criar(new CategoriaForm("Extensao Vinculada"));
        eventoService.criar(new EventoForm(
            "Evento Vinculado", "Desc", "Campus",
            hoje.plusDays(5), hoje.plusDays(5), vinculada.getId()
        ), org.getId());

        mockMvc.perform(post("/admin/categorias/" + vinculada.getId() + "/excluir")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/categorias"))
            .andExpect(flash().attributeExists("mensagemErro"));
        assertThat(categoriaRepository.existsById(vinculada.getId())).isTrue();

        mockMvc.perform(post("/admin/categorias/" + criada.getId() + "/excluir")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/categorias"))
            .andExpect(flash().attributeExists("mensagemSucesso"));
        assertThat(categoriaRepository.existsById(criada.getId())).isFalse();
    }

    @Test
    void rf04DeveListarSemHashConcederERemoverOrganizadorPreservandoParticipanteEAdmin() throws Exception {
        CustomUserDetails adminPrincipal = carregarPrincipal("admin@eventif.edu.br");
        Usuario admin = usuarioRepository.findWithPerfisByEmailIgnoreCase("admin@eventif.edu.br").orElseThrow();
        Usuario participante = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Aluno Admin Test", "aluno.adm@eventif.edu.br", "Senha@123")
        );

        mockMvc.perform(get("/admin/usuarios").with(user(adminPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("aluno.adm@eventif.edu.br")))
            .andExpect(content().string(not(containsString("$2a$"))));

        mockMvc.perform(post("/admin/usuarios/" + participante.getId() + "/conceder-organizador")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        Usuario aposConcessao = usuarioRepository.findByIdComPerfis(participante.getId()).orElseThrow();
        assertThat(aposConcessao.getPerfis())
            .extracting(Perfil::getNome)
            .containsExactlyInAnyOrder(PerfilConstants.ROLE_PARTICIPANTE, PerfilConstants.ROLE_ORGANIZADOR);

        mockMvc.perform(post("/admin/usuarios/" + participante.getId() + "/remover-organizador")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        Usuario aposRemocao = usuarioRepository.findByIdComPerfis(participante.getId()).orElseThrow();
        assertThat(aposRemocao.getPerfis())
            .extracting(Perfil::getNome)
            .containsExactly(PerfilConstants.ROLE_PARTICIPANTE);

        mockMvc.perform(post("/admin/usuarios/" + admin.getId() + "/remover-organizador")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"))
            .andExpect(flash().attributeExists("mensagemErro"));

        mockMvc.perform(post("/admin/usuarios/" + admin.getId() + "/conceder-organizador")
                .with(user(adminPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"))
            .andExpect(flash().attributeExists("mensagemErro"));

        Usuario adminApos = usuarioRepository.findByIdComPerfis(admin.getId()).orElseThrow();
        assertThat(adminApos.getPerfis())
            .extracting(Perfil::getNome)
            .containsExactly(PerfilConstants.ROLE_ADMIN);
    }

    @Test
    void naoAdminNaoDeveAcessarRotasAdministrativasNemElevarProprioPerfil() throws Exception {
        Usuario participante = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Aluno Comum", "comum.adm@eventif.edu.br", "Senha@123")
        );
        Usuario organizador = criarOrganizador("org.semadmin@eventif.edu.br");

        CustomUserDetails partPrincipal = carregarPrincipal(participante.getEmail());
        CustomUserDetails orgPrincipal = carregarPrincipal(organizador.getEmail());

        mockMvc.perform(post("/admin/usuarios/" + participante.getId() + "/conceder-organizador")
                .with(user(partPrincipal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        assertThat(usuarioRepository.findByIdComPerfis(participante.getId()).orElseThrow().getPerfis())
            .extracting(Perfil::getNome)
            .containsExactly(PerfilConstants.ROLE_PARTICIPANTE);

        mockMvc.perform(post("/admin/categorias")
                .param("nome", "Categoria Indevida")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/usuarios/" + participante.getId() + "/conceder-organizador")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/categorias"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    private Usuario criarOrganizador(String email) {
        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_ORGANIZADOR)));
        Usuario organizador = new Usuario(
            "Organizador Admin Test",
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
