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
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.enums.TipoAtividade;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.EventoRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizadorModuloEIdorControllerTest {

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
    private EventoRepository eventoRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void rf05ERf06DevemCriarEditarPublicarEncerrarEExcluirEventoProprio() throws Exception {
        Usuario org1 = criarOrganizador("org1.ev@eventif.edu.br");
        Usuario org2 = criarOrganizador("org2.ev@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Tecnologia"));
        CustomUserDetails org1Principal = carregarPrincipal(org1.getEmail());

        mockMvc.perform(post("/organizador/eventos")
                .param("titulo", "Semana Invalida")
                .param("descricao", "Desc")
                .param("local", "Campus")
                .param("dataInicio", "2026-12-10")
                .param("dataFim", "2026-12-08")
                .param("categoriaId", cat.getId().toString())
                .with(user(org1Principal))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("organizador/evento-form"))
            .andExpect(model().attributeExists("erroNegocio"));

        mockMvc.perform(post("/organizador/eventos")
                .param("titulo", "Semana Academica IFCE")
                .param("descricao", "Evento completo")
                .param("local", "Auditorio Central")
                .param("dataInicio", "2026-12-10")
                .param("dataFim", "2026-12-12")
                .param("categoriaId", cat.getId().toString())
                .param("organizadorId", org2.getId().toString())
                .with(user(org1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        List<Evento> eventosOrg1 = eventoRepository.findByOrganizadorIdOrderByDataInicioDescIdDesc(org1.getId());
        assertThat(eventosOrg1).hasSize(1);
        Evento criado = eventosOrg1.getFirst();
        assertThat(criado.getStatus()).isEqualTo(StatusEvento.RASCUNHO);
        assertThat(criado.getOrganizador().getId()).isEqualTo(org1.getId());

        mockMvc.perform(get("/organizador/eventos/" + criado.getId() + "/editar").with(user(org1Principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Semana Academica IFCE")));

        mockMvc.perform(post("/organizador/eventos/" + criado.getId())
                .param("titulo", "Semana Academica Atualizada")
                .param("descricao", "Evento atualizado")
                .param("local", "Auditorio Bloco A")
                .param("dataInicio", "2026-12-10")
                .param("dataFim", "2026-12-12")
                .param("categoriaId", cat.getId().toString())
                .with(user(org1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos"));

        mockMvc.perform(post("/organizador/eventos/" + criado.getId() + "/publicar")
                .with(user(org1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos"));

        assertThat(eventoRepository.findById(criado.getId()).orElseThrow().getStatus())
            .isEqualTo(StatusEvento.PUBLICADO);

        mockMvc.perform(post("/organizador/eventos/" + criado.getId() + "/encerrar")
                .with(user(org1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos"));

        assertThat(eventoRepository.findById(criado.getId()).orElseThrow().getStatus())
            .isEqualTo(StatusEvento.ENCERRADO);

        Evento rascunhoExcluivel = eventoService.criar(new EventoForm(
            "Rascunho Temporario", "Desc", "Sala 2",
            LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 20), cat.getId()
        ), org1.getId());

        mockMvc.perform(post("/organizador/eventos/" + rascunhoExcluivel.getId() + "/excluir")
                .with(user(org1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos"));

        assertThat(eventoRepository.existsById(rascunhoExcluivel.getId())).isFalse();
    }

    @Test
    void rf07ERf08DevemGerenciarAtividadesEMarcarEDesmarcarPresencaViaCheckboxes() throws Exception {
        Usuario org = criarOrganizador("org.atv@eventif.edu.br");
        Usuario p1 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Aluno Um", "a1.org@eventif.edu.br", "Senha@123")
        );
        Usuario p2 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Aluno Dois", "a2.org@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Extensao"));

        Evento evento = eventoService.criar(new EventoForm(
            "Jornada IFCE", "Desc", "Fortaleza",
            LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 16), cat.getId()
        ), org.getId());
        CustomUserDetails orgPrincipal = carregarPrincipal(org.getEmail());

        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades")
                .param("titulo", "Oficina Fora do Periodo")
                .param("tipo", "OFICINA")
                .param("ministrante", "Prof. Teste")
                .param("inicio", "2026-12-17T09:00")
                .param("fim", "2026-12-17T11:00")
                .param("cargaHoraria", "2")
                .param("vagas", "20")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("organizador/atividades"))
            .andExpect(model().attributeExists("erroNegocio"));

        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades")
                .param("titulo", "Minicurso Spring")
                .param("tipo", "MINICURSO")
                .param("ministrante", "Prof. Teste")
                .param("inicio", "2026-12-15T08:00")
                .param("fim", "2026-12-15T12:00")
                .param("cargaHoraria", "4")
                .param("vagas", "30")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos/" + evento.getId() + "/atividades"));

        Atividade criada = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(evento.getId()).getFirst();

        mockMvc.perform(get("/organizador/eventos/" + evento.getId() + "/atividades/" + criada.getId() + "/editar")
                .with(user(orgPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Minicurso Spring")));

        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades/" + criada.getId())
                .param("titulo", "Minicurso Spring Boot 3")
                .param("tipo", "MINICURSO")
                .param("ministrante", "Prof. Atualizado")
                .param("inicio", "2026-12-15T08:00")
                .param("fim", "2026-12-15T12:00")
                .param("cargaHoraria", "4")
                .param("vagas", "35")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos/" + evento.getId() + "/atividades"));

        eventoService.publicar(evento.getId(), org.getId());
        Inscricao insc1 = inscricaoService.inscrever(criada.getId(), p1.getId());
        Inscricao insc2 = inscricaoService.inscrever(criada.getId(), p2.getId());

        mockMvc.perform(get("/organizador/eventos/" + evento.getId() + "/atividades/" + criada.getId() + "/inscritos")
                .with(user(orgPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Aluno Um")))
            .andExpect(content().string(containsString("Aluno Dois")));

        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades/" + criada.getId() + "/inscritos")
                .param("presentesIds", insc1.getId().toString())
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos/" + evento.getId() + "/atividades/" + criada.getId() + "/inscritos"));

        assertThat(inscricaoRepository.findById(insc1.getId()).orElseThrow().isPresente()).isTrue();
        assertThat(inscricaoRepository.findById(insc2.getId()).orElseThrow().isPresente()).isFalse();

        // Submissao com todos os checkboxes desmarcados (sem parametro presentesIds)
        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades/" + criada.getId() + "/inscritos")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());

        assertThat(inscricaoRepository.findById(insc1.getId()).orElseThrow().isPresente()).isFalse();
        assertThat(inscricaoRepository.findById(insc2.getId()).orElseThrow().isPresente()).isFalse();

        Atividade atvSemInscritos = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Palestra Excluivel", TipoAtividade.PALESTRA, "Prof. C",
            LocalDateTime.of(2026, 12, 16, 10, 0), LocalDateTime.of(2026, 12, 16, 12, 0), 2, 40
        ), org.getId());

        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades/" + atvSemInscritos.getId() + "/excluir")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(atividadeRepository.existsById(atvSemInscritos.getId())).isFalse();

        // Suporte a carga horaria fracionada (ponto e virgula) e mensagem amigavel em typeMismatch
        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades")
                .param("titulo", "Oficina Horario Quebrado")
                .param("tipo", "OFICINA")
                .param("ministrante", "Prof. Decimal")
                .param("inicio", "2026-12-16T10:30")
                .param("fim", "2026-12-16T12:00")
                .param("cargaHoraria", "1.5")
                .param("vagas", "20")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos/" + evento.getId() + "/atividades"));

        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades")
                .param("titulo", "Palestra Virgula")
                .param("tipo", "PALESTRA")
                .param("ministrante", "Prof. Virgula")
                .param("inicio", "2026-12-16T14:00")
                .param("fim", "2026-12-16T15:30")
                .param("cargaHoraria", "1,5")
                .param("vagas", "25")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/organizador/eventos/" + evento.getId() + "/atividades"));

        mockMvc.perform(get("/organizador/eventos/" + evento.getId() + "/atividades").with(user(orgPrincipal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5h")));

        mockMvc.perform(post("/organizador/eventos/" + evento.getId() + "/atividades")
                .param("titulo", "Atividade Texto Invalido")
                .param("tipo", "PALESTRA")
                .param("ministrante", "Prof. Erro")
                .param("inicio", "2026-12-16T16:00")
                .param("fim", "2026-12-16T17:00")
                .param("cargaHoraria", "abc")
                .param("vagas", "20")
                .with(user(orgPrincipal))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("atividadeForm", "cargaHoraria"))
            .andExpect(content().string(containsString("Informe uma carga horária")))
            .andExpect(content().string(not(containsString("Failed to convert"))));
    }

    @Test
    void rs05TrocarIdDeEventoOuAtividadeDeOutroOrganizadorNaUrlDeveRetornar403() throws Exception {
        Usuario org1 = criarOrganizador("org1.idor.ctrl@eventif.edu.br");
        Usuario org2 = criarOrganizador("org2.idor.ctrl@eventif.edu.br");
        Usuario part = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Part IDOR", "part.idor.ctrl@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Seguranca IDOR"));

        Evento eventoOrg1 = eventoService.criar(new EventoForm(
            "Evento Privado Org1", "Descricao", "Campus",
            LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 19), cat.getId()
        ), org1.getId());
        Atividade atvOrg1 = atividadeService.criar(eventoOrg1.getId(), new AtividadeForm(
            "Atividade Org1", TipoAtividade.PALESTRA, "Org1",
            LocalDateTime.of(2026, 12, 18, 9, 0), LocalDateTime.of(2026, 12, 18, 11, 0), 2, 20
        ), org1.getId());
        eventoService.publicar(eventoOrg1.getId(), org1.getId());
        Inscricao inscOrg1 = inscricaoService.inscrever(atvOrg1.getId(), part.getId());

        Evento eventoOrg2 = eventoService.criar(new EventoForm(
            "Evento Org2", "Descricao", "Campus",
            LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 21), cat.getId()
        ), org2.getId());
        Atividade atvOrg2 = atividadeService.criar(eventoOrg2.getId(), new AtividadeForm(
            "Atividade Org2", TipoAtividade.OFICINA, "Org2",
            LocalDateTime.of(2026, 12, 20, 14, 0), LocalDateTime.of(2026, 12, 20, 16, 0), 2, 20
        ), org2.getId());
        eventoService.publicar(eventoOrg2.getId(), org2.getId());

        CustomUserDetails org2Principal = carregarPrincipal(org2.getEmail());

        mockMvc.perform(get("/organizador/eventos/" + eventoOrg1.getId() + "/editar").with(user(org2Principal)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId())
                .param("titulo", "")
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId() + "/publicar")
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId() + "/encerrar")
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId() + "/excluir")
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/organizador/eventos/" + eventoOrg1.getId() + "/atividades").with(user(org2Principal)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId() + "/atividades")
                .param("titulo", "Invasao")
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/organizador/eventos/" + eventoOrg1.getId() + "/atividades/" + atvOrg1.getId() + "/editar")
                .with(user(org2Principal)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId() + "/atividades/" + atvOrg1.getId())
                .param("titulo", "Invasao")
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId() + "/atividades/" + atvOrg1.getId() + "/excluir")
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/organizador/eventos/" + eventoOrg1.getId() + "/atividades/" + atvOrg1.getId() + "/inscritos")
                .with(user(org2Principal)))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg1.getId() + "/atividades/" + atvOrg1.getId() + "/inscritos")
                .param("presentesIds", inscOrg1.getId().toString())
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        // Tentativa cruzada: evento proprio de org2 + atividade de org1
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg2.getId() + "/atividades/" + atvOrg1.getId() + "/inscritos")
                .param("presentesIds", inscOrg1.getId().toString())
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        // Tentativa de enviar inscricao de org1 na chamada de atividade de org2
        mockMvc.perform(post("/organizador/eventos/" + eventoOrg2.getId() + "/atividades/" + atvOrg2.getId() + "/inscritos")
                .param("presentesIds", inscOrg1.getId().toString())
                .with(user(org2Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());

        assertThat(eventoRepository.findById(eventoOrg1.getId()).orElseThrow().getTitulo())
            .isEqualTo("Evento Privado Org1");
        assertThat(inscricaoRepository.findById(inscOrg1.getId()).orElseThrow().isPresente())
            .isFalse();
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
