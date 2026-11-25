package br.edu.ifce.eventif.security;

import br.edu.ifce.eventif.config.DataInitializer;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.enums.TipoAtividade;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.CategoriaRepository;
import br.edu.ifce.eventif.repository.EventoRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

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
    private DataInitializer dataInitializer;

    @Autowired
    private Clock clock;

    private Long eventoPublicadoId;
    private Long atividadePublicadaId;
    private Long eventoEncerradoId;

    @BeforeEach
    void prepararDadosDeSeguranca() {
        Perfil perfilParticipante = perfilRepository.findByNome(PerfilConstants.ROLE_PARTICIPANTE)
            .orElseThrow();
        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseThrow();

        Usuario participante = new Usuario(
            "Participante Matriz",
            "part@eventif.edu.br",
            passwordEncoder.encode("Senha@123"),
            true
        );
        participante.adicionarPerfil(perfilParticipante);
        participante = usuarioRepository.save(participante);

        Usuario organizador = new Usuario(
            "Organizador Matriz",
            "org@eventif.edu.br",
            passwordEncoder.encode("Senha@123"),
            true
        );
        organizador.adicionarPerfil(perfilOrganizador);
        organizador = usuarioRepository.save(organizador);

        LocalDate hoje = LocalDate.now(clock);
        Categoria categoria = categoriaRepository.save(new Categoria("Seguranca"));

        Evento publicado = eventoRepository.save(new Evento(
            "Evento Publicado Matriz",
            "Descricao do evento publicado",
            "Auditorio",
            hoje.plusDays(10),
            hoje.plusDays(11),
            StatusEvento.PUBLICADO,
            categoria,
            organizador
        ));
        eventoPublicadoId = publicado.getId();

        Atividade atividadePublicada = atividadeRepository.save(new Atividade(
            "Palestra Matriz",
            TipoAtividade.PALESTRA,
            "Prof. Matriz",
            hoje.plusDays(10).atTime(9, 0),
            hoje.plusDays(10).atTime(11, 0),
            2,
            30,
            publicado
        ));
        atividadePublicadaId = atividadePublicada.getId();

        Evento encerrado = eventoRepository.save(new Evento(
            "Evento Encerrado Matriz",
            "Descricao do evento encerrado",
            "Campus",
            hoje.minusDays(5),
            hoje.minusDays(4),
            StatusEvento.ENCERRADO,
            categoria,
            organizador
        ));
        eventoEncerradoId = encerrado.getId();

        Atividade atividadeEncerrada = atividadeRepository.save(new Atividade(
            "Oficina Encerrada Matriz",
            TipoAtividade.OFICINA,
            "Prof. Matriz",
            hoje.minusDays(5).atTime(14, 0),
            hoje.minusDays(5).atTime(16, 0),
            2,
            20,
            encerrado
        ));

        inscricaoRepository.save(new Inscricao(
            hoje.minusDays(6).atTime(10, 0),
            true,
            participante,
            atividadeEncerrada
        ));
    }

    @Test
    void deveCriarAdminInicialComBCryptDeFormaIdempotente() {
        long totalUsuariosAntes = usuarioRepository.count();
        long totalPerfisAntes = perfilRepository.count();

        Usuario admin = usuarioRepository.findWithPerfisByEmailIgnoreCase("admin@eventif.edu.br")
            .orElseThrow();

        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.getSenha()).isNotEqualTo("AdminDemo@123");
        assertThat(passwordEncoder.matches("AdminDemo@123", admin.getSenha())).isTrue();
        assertThat(admin.getPerfis())
            .extracting(Perfil::getNome)
            .contains(PerfilConstants.ROLE_ADMIN);

        dataInitializer.run(null);

        assertThat(usuarioRepository.count()).isEqualTo(totalUsuariosAntes);
        assertThat(perfilRepository.count()).isEqualTo(totalPerfisAntes);
    }

    @Test
    void deveAutenticarUsuarioAtivoPorEmailERejeitarSenhaIncorretaOuUsuarioInativo() throws Exception {
        Perfil perfilParticipante = perfilRepository.findByNome(PerfilConstants.ROLE_PARTICIPANTE)
            .orElseThrow();

        Usuario ativo = new Usuario(
            "Participante Ativo",
            "ativo@eventif.edu.br",
            passwordEncoder.encode("SenhaForte@123"),
            true
        );
        ativo.adicionarPerfil(perfilParticipante);
        usuarioRepository.save(ativo);

        Usuario inativo = new Usuario(
            "Participante Inativo",
            "inativo@eventif.edu.br",
            passwordEncoder.encode("SenhaForte@123"),
            false
        );
        inativo.adicionarPerfil(perfilParticipante);
        usuarioRepository.save(inativo);

        mockMvc.perform(post("/login")
                .param("email", "ativo@eventif.edu.br")
                .param("senha", "SenhaForte@123")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(authenticated().withUsername("ativo@eventif.edu.br"));

        mockMvc.perform(post("/login")
                .param("email", "ativo@eventif.edu.br")
                .param("senha", "SenhaErrada")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?erro"))
            .andExpect(unauthenticated());

        mockMvc.perform(post("/login")
                .param("email", "inativo@eventif.edu.br")
                .param("senha", "SenhaForte@123")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?erro"))
            .andExpect(unauthenticated());

        mockMvc.perform(post("/logout").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"))
            .andExpect(unauthenticated());

        mockMvc.perform(post("/logout"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonimoDeveAcessarRotasPublicasESerRedirecionadoParaLoginNasProtegidas() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/eventos/" + eventoPublicadoId)).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/cadastro")).andExpect(status().isOk());

        mockMvc.perform(get("/inscricoes"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/inscricoes/atividades/" + atividadePublicadaId).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/certificados/eventos/" + eventoEncerradoId))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/organizador/eventos"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/categorias"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/usuarios"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "part@eventif.edu.br", roles = "PARTICIPANTE")
    void participanteDeveAcessarRotasDeParticipanteEBloquearOrganizadorEAdmin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/eventos/" + eventoPublicadoId)).andExpect(status().isOk());
        mockMvc.perform(get("/inscricoes")).andExpect(status().isOk());
        mockMvc.perform(post("/inscricoes/atividades/" + atividadePublicadaId).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + eventoPublicadoId));
        mockMvc.perform(get("/certificados/eventos/" + eventoEncerradoId)).andExpect(status().isOk());

        mockMvc.perform(get("/organizador/eventos")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/categorias")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/usuarios")).andExpect(status().isForbidden());

        mockMvc.perform(get("/"))
            .andExpect(content().string(containsString("Minhas inscrições")))
            .andExpect(content().string(not(containsString("Meus eventos"))))
            .andExpect(content().string(not(containsString("Categorias"))));
    }

    @Test
    @WithMockUser(username = "org@eventif.edu.br", roles = "ORGANIZADOR")
    void organizadorDeveAcessarRotasDeOrganizadorEBloquearParticipanteEAdmin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/eventos/" + eventoPublicadoId)).andExpect(status().isOk());
        mockMvc.perform(get("/organizador/eventos")).andExpect(status().isOk());

        mockMvc.perform(get("/inscricoes")).andExpect(status().isForbidden());
        mockMvc.perform(post("/inscricoes/atividades/" + atividadePublicadaId).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(get("/certificados/eventos/" + eventoEncerradoId)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/categorias")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/usuarios")).andExpect(status().isForbidden());

        mockMvc.perform(get("/"))
            .andExpect(content().string(containsString("Meus eventos")))
            .andExpect(content().string(not(containsString("Minhas inscrições"))))
            .andExpect(content().string(not(containsString("Categorias"))));
    }

    @Test
    @WithMockUser(username = "admin@eventif.edu.br", roles = "ADMIN")
    void adminDeveAcessarRotasAdminEBloquearParticipanteEOrganizador() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/eventos/" + eventoPublicadoId)).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/usuarios")).andExpect(status().isOk());

        mockMvc.perform(get("/inscricoes")).andExpect(status().isForbidden());
        mockMvc.perform(post("/inscricoes/atividades/" + atividadePublicadaId).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(get("/certificados/eventos/" + eventoEncerradoId)).andExpect(status().isForbidden());
        mockMvc.perform(get("/organizador/eventos")).andExpect(status().isForbidden());

        mockMvc.perform(get("/"))
            .andExpect(content().string(containsString("Categorias")))
            .andExpect(content().string(containsString("Usuários")))
            .andExpect(content().string(not(containsString("Minhas inscrições"))))
            .andExpect(content().string(not(containsString("Meus eventos"))));
    }
}
