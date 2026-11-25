package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.AtividadeForm;
import br.edu.ifce.eventif.dto.CadastroUsuarioForm;
import br.edu.ifce.eventif.dto.CategoriaForm;
import br.edu.ifce.eventif.dto.CertificadoView;
import br.edu.ifce.eventif.dto.EventoForm;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.enums.TipoAtividade;
import br.edu.ifce.eventif.exception.AcessoNegadoPropriedadeException;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.ListaEsperaRepository;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import br.edu.ifce.eventif.security.PerfilConstants;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegrasNegocioServiceTest {

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
    private CertificadoService certificadoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private ListaEsperaRepository listaEsperaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void rn01ERf01DevemCadastrarParticipanteComBCryptEBloquearEmailDuplicado() {
        Usuario cadastrado = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Aluno Novo", "Aluno@EventIF.edu.br", "Senha@123")
        );

        assertThat(cadastrado.getEmail()).isEqualTo("aluno@eventif.edu.br");
        assertThat(passwordEncoder.matches("Senha@123", cadastrado.getSenha())).isTrue();
        assertThat(cadastrado.getPerfis())
            .extracting(Perfil::getNome)
            .containsExactly(PerfilConstants.ROLE_PARTICIPANTE);

        assertThatThrownBy(() -> usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Outro Aluno", "ALUNO@eventif.edu.br", "Outr@123")
        )).isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("e-mail");
    }

    @Test
    void rn02DeveValidarDatasDeEventoEDeAtividadeDentroDoPeriodo() {
        Usuario org = criarOrganizador("org.datas@eventif.edu.br");
        Categoria cat = categoriaService.criar(new CategoriaForm("Computacao"));

        assertThatThrownBy(() -> eventoService.criar(
            new EventoForm(
                "Evento Invalido",
                "Descricao",
                "Campus",
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 9),
                cat.getId()
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class);

        Evento eventoUmDia = eventoService.criar(
            new EventoForm(
                "Jornada 1 Dia",
                "Descricao",
                "Auditorio",
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 10),
                cat.getId()
            ),
            org.getId()
        );
        assertThat(eventoUmDia.getStatus()).isEqualTo(StatusEvento.RASCUNHO);

        assertThatThrownBy(() -> atividadeService.criar(
            eventoUmDia.getId(),
            new AtividadeForm(
                "Horario Invertido",
                TipoAtividade.PALESTRA,
                "Ministrante",
                LocalDateTime.of(2026, 12, 10, 14, 0),
                LocalDateTime.of(2026, 12, 10, 14, 0),
                2,
                30
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class);

        assertThatThrownBy(() -> atividadeService.criar(
            eventoUmDia.getId(),
            new AtividadeForm(
                "Fora do Periodo",
                TipoAtividade.MINICURSO,
                "Ministrante",
                LocalDateTime.of(2026, 12, 11, 9, 0),
                LocalDateTime.of(2026, 12, 11, 11, 0),
                2,
                30
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class);

        atividadeService.criar(
            eventoUmDia.getId(),
            new AtividadeForm(
                "Palestra Valida",
                TipoAtividade.PALESTRA,
                "Ministrante",
                LocalDateTime.of(2026, 12, 10, 9, 0),
                LocalDateTime.of(2026, 12, 10, 11, 0),
                2,
                30
            ),
            org.getId()
        );

        assertThatThrownBy(() -> eventoService.atualizar(
            eventoUmDia.getId(),
            new EventoForm(
                "Jornada Alterada",
                "Descricao",
                "Auditorio",
                LocalDate.of(2026, 12, 12),
                LocalDate.of(2026, 12, 13),
                cat.getId()
            ),
            org.getId()
        )).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rn03Rn04ERn05DevemValidarStatusPublicadoDuplicidadeELimitacaoDeVagas() {
        Usuario org = criarOrganizador("org.insc@eventif.edu.br");
        Usuario p1 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Part 1", "p1.insc@eventif.edu.br", "Senha@123")
        );
        Usuario p2 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Part 2", "p2.insc@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Sistemas"));

        Evento evento = eventoService.criar(
            new EventoForm(
                "Semana Tech",
                "Descricao",
                "IFCE",
                LocalDate.of(2026, 12, 15),
                LocalDate.of(2026, 12, 16),
                cat.getId()
            ),
            org.getId()
        );
        Atividade atividade = atividadeService.criar(
            evento.getId(),
            new AtividadeForm(
                "Oficina Spring",
                TipoAtividade.OFICINA,
                "Prof. Teste",
                LocalDateTime.of(2026, 12, 15, 14, 0),
                LocalDateTime.of(2026, 12, 15, 16, 0),
                2,
                1
            ),
            org.getId()
        );

        assertThatThrownBy(() -> inscricaoService.inscrever(atividade.getId(), p1.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("publicados");

        eventoService.publicar(evento.getId(), org.getId());

        Inscricao inscricao = inscricaoService.inscrever(atividade.getId(), p1.getId());
        assertThat(inscricao.getId()).isNotNull();

        assertThatThrownBy(() -> inscricaoService.inscrever(atividade.getId(), p1.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("já está inscrito");

        assertThatThrownBy(() -> inscricaoService.inscrever(atividade.getId(), p2.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("vagas");

        eventoService.encerrar(evento.getId(), org.getId());
        assertThatThrownBy(() -> inscricaoService.inscrever(atividade.getId(), p2.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("publicados");
    }

    @Test
    void rn06DevePermitirCancelamentoAntesDoInicioEBloquearNoInicioOuDepois() {
        Usuario org = criarOrganizador("org.cancel@eventif.edu.br");
        Usuario p1 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Part Cancel", "p.cancel@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Redes"));

        Evento evento = eventoService.criar(
            new EventoForm(
                "Workshop Redes",
                "Descricao",
                "Lab 2",
                LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 20),
                cat.getId()
            ),
            org.getId()
        );
        Atividade atividade = atividadeService.criar(
            evento.getId(),
            new AtividadeForm(
                "Seguranca Web",
                TipoAtividade.PALESTRA,
                "Prof. Rede",
                LocalDateTime.of(2026, 12, 20, 14, 0),
                LocalDateTime.of(2026, 12, 20, 16, 0),
                2,
                20
            ),
            org.getId()
        );
        eventoService.publicar(evento.getId(), org.getId());

        ZoneId zone = ZoneId.of("America/Fortaleza");
        InscricaoService serviceAntes = new InscricaoService(
            inscricaoRepository,
            listaEsperaRepository,
            atividadeRepository,
            usuarioRepository,
            atividadeService,
            Clock.fixed(LocalDateTime.of(2026, 12, 20, 13, 59).atZone(zone).toInstant(), zone)
        );

        Inscricao inscricao1 = serviceAntes.inscrever(atividade.getId(), p1.getId());
        serviceAntes.cancelar(inscricao1.getId(), p1.getId());
        assertThat(inscricaoRepository.existsById(inscricao1.getId())).isFalse();

        Inscricao inscricao2 = serviceAntes.inscrever(atividade.getId(), p1.getId());
        InscricaoService serviceNoHorario = new InscricaoService(
            inscricaoRepository,
            listaEsperaRepository,
            atividadeRepository,
            usuarioRepository,
            atividadeService,
            Clock.fixed(LocalDateTime.of(2026, 12, 20, 14, 0).atZone(zone).toInstant(), zone)
        );

        assertThatThrownBy(() -> serviceNoHorario.cancelar(inscricao2.getId(), p1.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("antes do início");
    }

    @Test
    void rn07ERf14DevemExigirEventoEncerradoEPresencaESomarSomenteCargaHorariaComPresenca() {
        Usuario org = criarOrganizador("org.cert@eventif.edu.br");
        Usuario p1 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Aluno Cert", "p.cert@eventif.edu.br", "Senha@123")
        );
        Usuario pSemPresenca = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Aluno Falta", "p.falta@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Pesquisa"));

        Evento evento = eventoService.criar(
            new EventoForm(
                "Congresso IFCE",
                "Descricao",
                "Auditorio",
                LocalDate.of(2026, 12, 22),
                LocalDate.of(2026, 12, 22),
                cat.getId()
            ),
            org.getId()
        );
        Atividade atv1 = atividadeService.criar(
            evento.getId(),
            new AtividadeForm(
                "Minicurso JPA",
                TipoAtividade.MINICURSO,
                "Prof. A",
                LocalDateTime.of(2026, 12, 22, 8, 0),
                LocalDateTime.of(2026, 12, 22, 12, 0),
                4,
                30
            ),
            org.getId()
        );
        Atividade atv2 = atividadeService.criar(
            evento.getId(),
            new AtividadeForm(
                "Palestra Docker",
                TipoAtividade.PALESTRA,
                "Prof. B",
                LocalDateTime.of(2026, 12, 22, 14, 0),
                LocalDateTime.of(2026, 12, 22, 16, 0),
                2,
                50
            ),
            org.getId()
        );
        eventoService.publicar(evento.getId(), org.getId());

        Inscricao inscP1Atv1 = inscricaoService.inscrever(atv1.getId(), p1.getId());
        inscricaoService.inscrever(atv2.getId(), p1.getId());
        inscricaoService.inscrever(atv1.getId(), pSemPresenca.getId());

        inscricaoService.registrarPresencas(evento.getId(), atv1.getId(), List.of(inscP1Atv1.getId()), org.getId());
        inscricaoService.registrarPresencas(evento.getId(), atv2.getId(), null, org.getId());

        assertThatThrownBy(() -> certificadoService.emitirCertificado(evento.getId(), p1.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("encerramento");

        eventoService.encerrar(evento.getId(), org.getId());

        assertThatThrownBy(() -> certificadoService.emitirCertificado(evento.getId(), pSemPresenca.getId()))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("presença");

        CertificadoView certificado = certificadoService.emitirCertificado(evento.getId(), p1.getId());
        assertThat(certificado.participanteNome()).isEqualTo("Aluno Cert");
        assertThat(certificado.eventoTitulo()).isEqualTo("Congresso IFCE");
        assertThat(certificado.cargaHorariaTotal()).isEqualTo(4);
        assertThat(certificado.atividadesPresentes()).hasSize(1);
    }

    @Test
    void rs05ERs06DevemBloquearAcessoDeOutroOrganizadorOuOutroParticipante() {
        Usuario org1 = criarOrganizador("org1.idor@eventif.edu.br");
        Usuario org2 = criarOrganizador("org2.idor@eventif.edu.br");
        Usuario part1 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Part 1", "p1.idor@eventif.edu.br", "Senha@123")
        );
        Usuario part2 = usuarioService.cadastrarParticipante(
            new CadastroUsuarioForm("Part 2", "p2.idor@eventif.edu.br", "Senha@123")
        );
        Categoria cat = categoriaService.criar(new CategoriaForm("Extensao"));

        Evento eventoOrg1 = eventoService.criar(
            new EventoForm(
                "Evento Exclusivo Org1",
                "Descricao",
                "Sala 10",
                LocalDate.of(2026, 12, 28),
                LocalDate.of(2026, 12, 28),
                cat.getId()
            ),
            org1.getId()
        );
        Atividade atvOrg1 = atividadeService.criar(
            eventoOrg1.getId(),
            new AtividadeForm(
                "Oficina Org1",
                TipoAtividade.OFICINA,
                "Org1",
                LocalDateTime.of(2026, 12, 28, 9, 0),
                LocalDateTime.of(2026, 12, 28, 11, 0),
                2,
                15
            ),
            org1.getId()
        );
        eventoService.publicar(eventoOrg1.getId(), org1.getId());
        Inscricao inscPart1 = inscricaoService.inscrever(atvOrg1.getId(), part1.getId());

        assertThatThrownBy(() -> eventoService.buscarEventoDoOrganizadorOu403(eventoOrg1.getId(), org2.getId()))
            .isInstanceOf(AcessoNegadoPropriedadeException.class);
        assertThatThrownBy(() -> eventoService.encerrar(eventoOrg1.getId(), org2.getId()))
            .isInstanceOf(AcessoNegadoPropriedadeException.class);
        assertThatThrownBy(() -> atividadeService.listarPorEventoDoOrganizador(eventoOrg1.getId(), org2.getId()))
            .isInstanceOf(AcessoNegadoPropriedadeException.class);
        assertThatThrownBy(() -> inscricaoService.registrarPresencas(
            eventoOrg1.getId(), atvOrg1.getId(), List.of(inscPart1.getId()), org2.getId()
        )).isInstanceOf(AcessoNegadoPropriedadeException.class);

        assertThatThrownBy(() -> inscricaoService.cancelar(inscPart1.getId(), part2.getId()))
            .isInstanceOf(AcessoNegadoPropriedadeException.class);
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
}
