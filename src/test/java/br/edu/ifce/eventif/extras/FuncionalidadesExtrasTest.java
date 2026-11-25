package br.edu.ifce.eventif.extras;

import br.edu.ifce.eventif.dto.AtividadeForm;
import br.edu.ifce.eventif.dto.CadastroUsuarioForm;
import br.edu.ifce.eventif.dto.CategoriaForm;
import br.edu.ifce.eventif.dto.EventoForm;
import br.edu.ifce.eventif.dto.MinhaListaEsperaView;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.ListaEspera;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.TipoAtividade;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.ListaEsperaRepository;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import br.edu.ifce.eventif.security.CustomUserDetails;
import br.edu.ifce.eventif.security.PerfilConstants;
import br.edu.ifce.eventif.service.AtividadeService;
import br.edu.ifce.eventif.service.CategoriaService;
import br.edu.ifce.eventif.service.EventoService;
import br.edu.ifce.eventif.service.InscricaoService;
import br.edu.ifce.eventif.service.UsuarioService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FuncionalidadesExtrasTest {

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
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private ListaEsperaRepository listaEsperaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Test
    void ex02DeveBloquearInscricaoComHorariosSobrepostosEPermitirHorariosAdjacentes() throws Exception {
        LocalDate dia = LocalDate.now(clock).plusDays(12);
        Usuario org = criarOrganizador("org.ex02@eventif.edu.br");
        Usuario part = criarParticipante("Aluno Horario", "part.ex02@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Extras Horario"));

        Evento evento1 = eventoService.criar(new EventoForm(
            "Evento Extras 1", "Desc", "Campus A", dia, dia, cat.getId()
        ), org.getId());
        Evento evento2 = eventoService.criar(new EventoForm(
            "Evento Extras 2", "Desc", "Campus B", dia, dia, cat.getId()
        ), org.getId());

        Atividade base = atividadeService.criar(evento1.getId(), new AtividadeForm(
            "Minicurso Base", TipoAtividade.MINICURSO, "Prof. Base",
            dia.atTime(8, 0), dia.atTime(10, 0), 2, 20
        ), org.getId());
        Atividade sobrepostaParcial = atividadeService.criar(evento1.getId(), new AtividadeForm(
            "Oficina Parcial", TipoAtividade.OFICINA, "Prof. Parcial",
            dia.atTime(9, 30), dia.atTime(11, 30), 2, 20
        ), org.getId());
        Atividade sobrepostaOutroEvento = atividadeService.criar(evento2.getId(), new AtividadeForm(
            "Palestra Concorrente", TipoAtividade.PALESTRA, "Prof. Outro",
            dia.atTime(8, 30), dia.atTime(9, 30), 1, 20
        ), org.getId());
        Atividade adjacente = atividadeService.criar(evento2.getId(), new AtividadeForm(
            "Oficina Adjacente", TipoAtividade.OFICINA, "Prof. Adj",
            dia.atTime(10, 0), dia.atTime(12, 0), 2, 20
        ), org.getId());

        eventoService.publicar(evento1.getId(), org.getId());
        eventoService.publicar(evento2.getId(), org.getId());

        CustomUserDetails partPrincipal = carregarPrincipal(part.getEmail());

        inscricaoService.inscrever(base.getId(), part.getId());

        mockMvc.perform(post("/inscricoes/atividades/" + sobrepostaParcial.getId())
                .with(user(partPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + evento1.getId()))
            .andExpect(flash().attribute("mensagemErro", containsString("Conflito de horário")));

        assertThatThrownBy(() -> inscricaoService.inscrever(sobrepostaOutroEvento.getId(), part.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("Conflito de horário");

        mockMvc.perform(post("/inscricoes/atividades/" + adjacente.getId())
                .with(user(partPrincipal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + evento2.getId()))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(part.getId(), adjacente.getId())).isTrue();
    }

    @Test
    void ex01DeveGerenciarListaDeEsperaBloquearTerceirosEPromoverAutomaticamenteEmOrdemFifoNoCancelamento()
        throws Exception {
        LocalDate dia = LocalDate.now(clock).plusDays(14);
        Usuario org = criarOrganizador("org.ex01@eventif.edu.br");
        Usuario p1 = criarParticipante("Participante Um", "p1.ex01@eventif.edu.br");
        Usuario p2 = criarParticipante("Participante Dois", "p2.ex01@eventif.edu.br");
        Usuario p3 = criarParticipante("Participante Tres", "p3.ex01@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Extras Fila"));

        Evento evento = eventoService.criar(new EventoForm(
            "Evento Fila Espera", "Desc", "Auditório", dia, dia, cat.getId()
        ), org.getId());
        Atividade atividade = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Oficina Disputada", TipoAtividade.OFICINA, "Prof. Fila",
            dia.atTime(14, 0), dia.atTime(16, 0), 2, 1
        ), org.getId());
        eventoService.publicar(evento.getId(), org.getId());

        assertThatThrownBy(() -> inscricaoService.entrarNaListaDeEspera(atividade.getId(), p2.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("vagas disponíveis");

        CustomUserDetails p1Principal = carregarPrincipal(p1.getEmail());
        CustomUserDetails p2Principal = carregarPrincipal(p2.getEmail());
        CustomUserDetails p3Principal = carregarPrincipal(p3.getEmail());

        Inscricao inscricaoP1 = inscricaoService.inscrever(atividade.getId(), p1.getId());

        mockMvc.perform(get("/eventos/" + evento.getId()).with(user(p2Principal)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Lista de espera")));

        mockMvc.perform(post("/inscricoes/atividades/" + atividade.getId() + "/espera")
                .with(user(p2Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/eventos/" + evento.getId()))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        ListaEspera esperaP3 = inscricaoService.entrarNaListaDeEspera(atividade.getId(), p3.getId());

        assertThatThrownBy(() -> inscricaoService.entrarNaListaDeEspera(atividade.getId(), p2.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("já está na lista de espera");

        List<MinhaListaEsperaView> filaP3Antes = inscricaoService.listarMinhaListaDeEspera(p3.getId());
        assertThat(filaP3Antes).hasSize(1);
        assertThat(filaP3Antes.getFirst().posicaoFila()).isEqualTo(2);

        mockMvc.perform(post("/inscricoes/espera/" + esperaP3.getId() + "/cancelar")
                .with(user(p2Principal))
                .with(csrf()))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/inscricoes/" + inscricaoP1.getId() + "/cancelar")
                .with(user(p1Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/inscricoes"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(p1.getId(), atividade.getId())).isFalse();
        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(p2.getId(), atividade.getId())).isTrue();
        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(p3.getId(), atividade.getId())).isFalse();
        assertThat(listaEsperaRepository.existsByUsuarioIdAndAtividadeId(p2.getId(), atividade.getId())).isFalse();
        assertThat(listaEsperaRepository.existsByUsuarioIdAndAtividadeId(p3.getId(), atividade.getId())).isTrue();

        List<MinhaListaEsperaView> filaP3Depois = inscricaoService.listarMinhaListaDeEspera(p3.getId());
        assertThat(filaP3Depois).hasSize(1);
        assertThat(filaP3Depois.getFirst().posicaoFila()).isEqualTo(1);

        mockMvc.perform(post("/inscricoes/espera/" + esperaP3.getId() + "/cancelar")
                .with(user(p3Principal))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/inscricoes"))
            .andExpect(flash().attributeExists("mensagemSucesso"));

        assertThat(listaEsperaRepository.existsById(esperaP3.getId())).isFalse();
    }

    @Test
    void ex01ComEx02PromocaoAutomaticaDevePularCandidatoComConflitoDeHorarioEPromoverProximoElegivel() {
        LocalDate dia = LocalDate.now(clock).plusDays(16);
        Usuario org = criarOrganizador("org.ex01ex02@eventif.edu.br");
        Usuario p1 = criarParticipante("Part Titular", "p1.ex01ex02@eventif.edu.br");
        Usuario p2ComConflito = criarParticipante("Part Conflitante", "p2.ex01ex02@eventif.edu.br");
        Usuario p3Elegivel = criarParticipante("Part Elegivel", "p3.ex01ex02@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Extras Integracao"));

        Evento evento = eventoService.criar(new EventoForm(
            "Evento Integracao Fila", "Desc", "Campus", dia, dia, cat.getId()
        ), org.getId());
        Atividade atvAlvo = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Oficina Alvo", TipoAtividade.OFICINA, "Prof. A",
            dia.atTime(9, 0), dia.atTime(11, 0), 2, 1
        ), org.getId());
        Atividade atvParalela = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Palestra Paralela", TipoAtividade.PALESTRA, "Prof. B",
            dia.atTime(10, 0), dia.atTime(12, 0), 2, 10
        ), org.getId());
        eventoService.publicar(evento.getId(), org.getId());

        Inscricao inscP1 = inscricaoService.inscrever(atvAlvo.getId(), p1.getId());
        inscricaoService.entrarNaListaDeEspera(atvAlvo.getId(), p2ComConflito.getId());
        inscricaoService.entrarNaListaDeEspera(atvAlvo.getId(), p3Elegivel.getId());

        inscricaoService.inscrever(atvParalela.getId(), p2ComConflito.getId());

        inscricaoService.cancelar(inscP1.getId(), p1.getId());

        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(p2ComConflito.getId(), atvAlvo.getId())).isFalse();
        assertThat(inscricaoRepository.existsByUsuarioIdAndAtividadeId(p3Elegivel.getId(), atvAlvo.getId())).isTrue();
        assertThat(listaEsperaRepository.existsByUsuarioIdAndAtividadeId(p2ComConflito.getId(), atvAlvo.getId())).isTrue();
        assertThat(listaEsperaRepository.existsByUsuarioIdAndAtividadeId(p3Elegivel.getId(), atvAlvo.getId())).isFalse();
    }

    @Test
    void ex03DeveGerarCertificadoEmPdfValidoComTextoExtraidoEBloquearNaoElegiveisENaoParticipantes()
        throws Exception {
        LocalDate dia = LocalDate.now(clock).plusDays(18);
        Usuario org = criarOrganizador("org.ex03@eventif.edu.br");
        Usuario partPresente = criarParticipante("Lucas Certificado", "lucas.ex03@eventif.edu.br");
        Usuario partAusente = criarParticipante("Marcos Ausente", "marcos.ex03@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Extras PDF"));

        Evento evento = eventoService.criar(new EventoForm(
            "Congresso PDF IFCE", "Evento com PDF", "Auditorio Central", dia, dia, cat.getId()
        ), org.getId());
        Atividade atvPresente = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Minicurso OpenPDF", TipoAtividade.MINICURSO, "Profa. PDF",
            dia.atTime(8, 0), dia.atTime(12, 0), 4, 20
        ), org.getId());
        Atividade atvFaltou = atividadeService.criar(evento.getId(), new AtividadeForm(
            "Oficina Ausente", TipoAtividade.OFICINA, "Prof. Falta",
            dia.atTime(14, 0), dia.atTime(16, 0), 2, 20
        ), org.getId());
        eventoService.publicar(evento.getId(), org.getId());

        Inscricao insc = inscricaoService.inscrever(atvPresente.getId(), partPresente.getId());
        inscricaoService.inscrever(atvFaltou.getId(), partPresente.getId());
        inscricaoService.inscrever(atvPresente.getId(), partAusente.getId());
        inscricaoService.registrarPresencas(evento.getId(), atvPresente.getId(), List.of(insc.getId()), org.getId());

        CustomUserDetails partPresentePrincipal = carregarPrincipal(partPresente.getEmail());
        CustomUserDetails partAusentePrincipal = carregarPrincipal(partAusente.getEmail());
        CustomUserDetails orgPrincipal = carregarPrincipal(org.getEmail());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId() + "/pdf").with(user(partPresentePrincipal)))
            .andExpect(status().isBadRequest());

        eventoService.encerrar(evento.getId(), org.getId());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId() + "/pdf").with(user(partAusentePrincipal)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/certificados/eventos/" + evento.getId() + "/pdf").with(user(orgPrincipal)))
            .andExpect(status().isForbidden());

        MvcResult result = mockMvc.perform(
                get("/certificados/eventos/" + evento.getId() + "/pdf").with(user(partPresentePrincipal))
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", containsString("certificado-evento-" + evento.getId() + ".pdf")))
            .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes.length).isGreaterThan(500);
        assertThat(new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            String textoPagina = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(textoPagina).contains("Lucas Certificado");
            assertThat(textoPagina).contains("Congresso PDF IFCE");
            assertThat(textoPagina).contains("Minicurso OpenPDF");
            assertThat(textoPagina).contains("4h");
            assertThat(textoPagina).doesNotContain("Oficina Ausente");
        }
    }

    private Usuario criarParticipante(String nome, String email) {
        return usuarioService.cadastrarParticipante(new CadastroUsuarioForm(nome, email, "Senha@123"));
    }

    private Usuario criarOrganizador(String email) {
        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_ORGANIZADOR)));
        Usuario organizador = new Usuario(
            "Organizador Extras",
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
