package br.edu.ifce.eventif.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.ifce.eventif.dto.CertificadoView;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.CategoriaRepository;
import br.edu.ifce.eventif.repository.EventoRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import br.edu.ifce.eventif.security.PerfilConstants;
import br.edu.ifce.eventif.service.CertificadoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataInitializerTest {

    @Autowired
    private DataInitializer dataInitializer;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CertificadoService certificadoService;

    @Value("${eventif.admin.email}")
    private String adminEmail;

    @Value("${eventif.admin.senha}")
    private String adminSenha;

    @Value("${eventif.demo.organizador.email}")
    private String organizadorEmail;

    @Value("${eventif.demo.organizador.senha}")
    private String organizadorSenha;

    @Value("${eventif.demo.organizador2.email}")
    private String organizador2Email;

    @Value("${eventif.demo.organizador2.senha}")
    private String organizador2Senha;

    @Value("${eventif.demo.participante.email}")
    private String participanteEmail;

    @Value("${eventif.demo.participante.senha}")
    private String participanteSenha;

    @Test
    void deveCarregarDadosDeDemonstracaoCoerentesNoStartup() {
        assertThat(perfilRepository.findByNome(PerfilConstants.ROLE_PARTICIPANTE)).isPresent();
        assertThat(perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)).isPresent();
        assertThat(perfilRepository.findByNome(PerfilConstants.ROLE_ADMIN)).isPresent();

        Usuario admin = usuarioRepository.findWithPerfisByEmailIgnoreCase(adminEmail).orElseThrow();
        Usuario organizador = usuarioRepository.findWithPerfisByEmailIgnoreCase(organizadorEmail).orElseThrow();
        Usuario organizador2 = usuarioRepository.findWithPerfisByEmailIgnoreCase(organizador2Email).orElseThrow();
        Usuario participante = usuarioRepository.findWithPerfisByEmailIgnoreCase(participanteEmail).orElseThrow();

        assertThat(passwordEncoder.matches(adminSenha, admin.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(organizadorSenha, organizador.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(organizador2Senha, organizador2.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(participanteSenha, participante.getSenha())).isTrue();

        assertThat(admin.getPerfis()).extracting("nome").contains(PerfilConstants.ROLE_ADMIN);
        assertThat(organizador.getPerfis()).extracting("nome").contains(PerfilConstants.ROLE_ORGANIZADOR);
        assertThat(organizador2.getPerfis()).extracting("nome").contains(PerfilConstants.ROLE_ORGANIZADOR);
        assertThat(participante.getPerfis()).extracting("nome").contains(PerfilConstants.ROLE_PARTICIPANTE);

        assertThat(categoriaRepository.findByNomeIgnoreCase(DataInitializer.CATEGORIA_COMPUTACAO)).isPresent();
        assertThat(categoriaRepository.findByNomeIgnoreCase(DataInitializer.CATEGORIA_PESQUISA)).isPresent();

        Evento eventoSemana = eventoRepository.findByTituloIgnoreCase(DataInitializer.EVENTO_SEMANA_TITULO).orElseThrow();
        Evento eventoSimposio = eventoRepository.findByTituloIgnoreCase(DataInitializer.EVENTO_SIMPOSIO_TITULO).orElseThrow();
        Evento eventoJornada = eventoRepository.findByTituloIgnoreCase(DataInitializer.EVENTO_JORNADA_ENCERRADO_TITULO).orElseThrow();

        assertThat(eventoSemana.getStatus()).isEqualTo(StatusEvento.PUBLICADO);
        assertThat(eventoSimposio.getStatus()).isEqualTo(StatusEvento.PUBLICADO);
        assertThat(eventoJornada.getStatus()).isEqualTo(StatusEvento.ENCERRADO);

        for (Evento evento : List.of(eventoSemana, eventoSimposio, eventoJornada)) {
            List<Atividade> atividades = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(evento.getId());
            assertThat(atividades).hasSize(2);
            for (Atividade atividade : atividades) {
                assertThat(atividade.getInicio()).isBefore(atividade.getFim());
                assertThat(atividade.getInicio().toLocalDate()).isBetween(evento.getDataInicio(), evento.getDataFim());
                assertThat(atividade.getFim().toLocalDate()).isBetween(evento.getDataInicio(), evento.getDataFim());
                assertThat(atividade.getCargaHoraria()).isPositive();
                assertThat(atividade.getVagas()).isPositive();
            }
        }

        CertificadoView certificado = certificadoService.emitirCertificado(eventoJornada.getId(), participante.getId());
        assertThat(certificado.cargaHorariaTotal()).isEqualTo(4);
        assertThat(certificado.atividadesPresentes()).hasSize(1);
    }

    @Test
    void deveSerIdempotenteQuandoExecutadoConsecutivamente() {
        long perfisAntes = perfilRepository.count();
        long usuariosAntes = usuarioRepository.count();
        long categoriasAntes = categoriaRepository.count();
        long eventosAntes = eventoRepository.count();
        long atividadesAntes = atividadeRepository.count();
        long inscricoesAntes = inscricaoRepository.count();

        dataInitializer.run(null);
        dataInitializer.run(null);

        assertThat(perfilRepository.count()).isEqualTo(perfisAntes);
        assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
        assertThat(categoriaRepository.count()).isEqualTo(categoriasAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
        assertThat(atividadeRepository.count()).isEqualTo(atividadesAntes);
        assertThat(inscricaoRepository.count()).isEqualTo(inscricoesAntes);
    }
}
