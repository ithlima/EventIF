package br.edu.ifce.eventif.config;

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
import br.edu.ifce.eventif.security.PerfilConstants;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializador idempotente de perfis base, administrador inicial (RS07)
 * e massa de dados de demonstração (RT04).
 */
@Component
public class DataInitializer implements ApplicationRunner {

    public static final String CATEGORIA_COMPUTACAO = "Computação e Tecnologia";
    public static final String CATEGORIA_PESQUISA = "Pesquisa e Extensão";

    public static final String EVENTO_SEMANA_TITULO = "Semana Acadêmica de Computação e Inovação";
    public static final String EVENTO_SIMPOSIO_TITULO = "Simpósio de Pesquisa Aplicada e Extensão Tecnológica";
    public static final String EVENTO_JORNADA_ENCERRADO_TITULO = "Jornada de Engenharia de Software do IFCE";

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final EventoRepository eventoRepository;
    private final AtividadeRepository atividadeRepository;
    private final InscricaoRepository inscricaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final String adminNome;
    private final String adminEmail;
    private final String adminSenha;
    private final boolean demoEnabled;
    private final String organizadorNome;
    private final String organizadorEmail;
    private final String organizadorSenha;
    private final String organizador2Nome;
    private final String organizador2Email;
    private final String organizador2Senha;
    private final String participanteNome;
    private final String participanteEmail;
    private final String participanteSenha;

    public DataInitializer(
        PerfilRepository perfilRepository,
        UsuarioRepository usuarioRepository,
        CategoriaRepository categoriaRepository,
        EventoRepository eventoRepository,
        AtividadeRepository atividadeRepository,
        InscricaoRepository inscricaoRepository,
        PasswordEncoder passwordEncoder,
        Clock clock,
        @Value("${eventif.admin.nome}") String adminNome,
        @Value("${eventif.admin.email}") String adminEmail,
        @Value("${eventif.admin.senha}") String adminSenha,
        @Value("${eventif.seed.demo-enabled:true}") boolean demoEnabled,
        @Value("${eventif.demo.organizador.nome}") String organizadorNome,
        @Value("${eventif.demo.organizador.email}") String organizadorEmail,
        @Value("${eventif.demo.organizador.senha}") String organizadorSenha,
        @Value("${eventif.demo.organizador2.nome}") String organizador2Nome,
        @Value("${eventif.demo.organizador2.email}") String organizador2Email,
        @Value("${eventif.demo.organizador2.senha}") String organizador2Senha,
        @Value("${eventif.demo.participante.nome}") String participanteNome,
        @Value("${eventif.demo.participante.email}") String participanteEmail,
        @Value("${eventif.demo.participante.senha}") String participanteSenha
    ) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.eventoRepository = eventoRepository;
        this.atividadeRepository = atividadeRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminNome = adminNome;
        this.adminEmail = adminEmail;
        this.adminSenha = adminSenha;
        this.demoEnabled = demoEnabled;
        this.organizadorNome = organizadorNome;
        this.organizadorEmail = organizadorEmail;
        this.organizadorSenha = organizadorSenha;
        this.organizador2Nome = organizador2Nome;
        this.organizador2Email = organizador2Email;
        this.organizador2Senha = organizador2Senha;
        this.participanteNome = participanteNome;
        this.participanteEmail = participanteEmail;
        this.participanteSenha = participanteSenha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Perfil perfilParticipante = garantirPerfil(PerfilConstants.ROLE_PARTICIPANTE);
        Perfil perfilOrganizador = garantirPerfil(PerfilConstants.ROLE_ORGANIZADOR);
        Perfil perfilAdmin = garantirPerfil(PerfilConstants.ROLE_ADMIN);

        garantirUsuario(adminNome, adminEmail, adminSenha, perfilAdmin);
        higienizarPerfisOrganizadorExclusivo();

        if (!demoEnabled) {
            return;
        }

        Usuario organizador = garantirUsuario(organizadorNome, organizadorEmail, organizadorSenha, perfilOrganizador);
        Usuario organizador2 = garantirUsuario(organizador2Nome, organizador2Email, organizador2Senha, perfilOrganizador);
        Usuario participante = garantirUsuario(participanteNome, participanteEmail, participanteSenha, perfilParticipante);

        Categoria computacao = garantirCategoria(CATEGORIA_COMPUTACAO);
        Categoria pesquisa = garantirCategoria(CATEGORIA_PESQUISA);

        LocalDate hoje = LocalDate.now(clock);
        LocalDateTime agora = LocalDateTime.now(clock);

        Evento eventoSemana = garantirEvento(
            EVENTO_SEMANA_TITULO,
            "Encontro anual com palestras, minicursos e oficinas voltados para desenvolvimento de software, arquitetura web e inteligência artificial no IFCE Campus Fortaleza.",
            "Auditório Central — IFCE Campus Fortaleza",
            hoje.plusDays(20),
            hoje.plusDays(22),
            StatusEvento.PUBLICADO,
            computacao,
            organizador
        );
        Atividade semanaPalestra = garantirAtividade(
            eventoSemana,
            "Abertura: Arquitetura Java Corporativa com Spring Boot",
            TipoAtividade.PALESTRA,
            "Profa. Dra. Helena Vasconcelos",
            hoje.plusDays(20).atTime(9, 0),
            hoje.plusDays(20).atTime(11, 0),
            2,
            80
        );
        garantirAtividade(
            eventoSemana,
            "Minicurso Prático de APIs e Segurança Web",
            TipoAtividade.MINICURSO,
            "Prof. Me. Rafael Nogueira",
            hoje.plusDays(21).atTime(14, 0),
            hoje.plusDays(21).atTime(18, 0),
            4,
            35
        );

        Evento eventoSimposio = garantirEvento(
            EVENTO_SIMPOSIO_TITULO,
            "Mostra de projetos de iniciação científica, extensão comunitária e oficinas práticas desenvolvidas por estudantes e docentes do Campus Fortaleza.",
            "Bloco de Pesquisa e Extensão — Sala 204",
            hoje.plusDays(35),
            hoje.plusDays(36),
            StatusEvento.PUBLICADO,
            pesquisa,
            organizador2
        );
        garantirAtividade(
            eventoSimposio,
            "Oficina de Escrita Científica e Submissão de Artigos",
            TipoAtividade.OFICINA,
            "Profa. Marina Organizadora",
            hoje.plusDays(35).atTime(14, 0),
            hoje.plusDays(35).atTime(17, 0),
            3,
            30
        );
        garantirAtividade(
            eventoSimposio,
            "Painel: Extensão Tecnológica e Impacto Social",
            TipoAtividade.PALESTRA,
            "Prof. Dr. Tiago Mendonça",
            hoje.plusDays(36).atTime(10, 0),
            hoje.plusDays(36).atTime(12, 0),
            2,
            60
        );

        Evento eventoJornada = garantirEvento(
            EVENTO_JORNADA_ENCERRADO_TITULO,
            "Edição encerrada dedicada a boas práticas de engenharia de software, testes automatizados e qualidade de código em sistemas acadêmicos.",
            "Laboratório de Sistemas 01 — IFCE Fortaleza",
            hoje.minusDays(15),
            hoje.minusDays(14),
            StatusEvento.ENCERRADO,
            computacao,
            organizador
        );
        Atividade jornadaOficina = garantirAtividade(
            eventoJornada,
            "Oficina de Testes Automatizados em Spring Boot",
            TipoAtividade.OFICINA,
            "Prof. Carlos Organizador",
            hoje.minusDays(15).atTime(13, 0),
            hoje.minusDays(15).atTime(17, 0),
            4,
            25
        );
        Atividade jornadaPalestra = garantirAtividade(
            eventoJornada,
            "Palestra de Encerramento: Observabilidade e DevOps",
            TipoAtividade.PALESTRA,
            "Eng. Lucas Ferreira",
            hoje.minusDays(14).atTime(15, 0),
            hoje.minusDays(14).atTime(18, 0),
            3,
            50
        );

        garantirInscricao(participante, semanaPalestra, agora.minusDays(1), false);
        garantirInscricao(participante, jornadaOficina, hoje.minusDays(16).atTime(10, 0), true);
        garantirInscricao(participante, jornadaPalestra, hoje.minusDays(16).atTime(10, 30), false);
    }

    private Perfil garantirPerfil(String nomePerfil) {
        return perfilRepository.findByNome(nomePerfil)
            .orElseGet(() -> perfilRepository.save(new Perfil(nomePerfil)));
    }

    private Usuario garantirUsuario(String nome, String email, String senhaPura, Perfil perfilObrigatorio) {
        String emailNormalizado = email.trim().toLowerCase();
        Usuario existente = usuarioRepository.findWithPerfisByEmailIgnoreCase(emailNormalizado).orElse(null);
        if (existente == null) {
            Usuario novoUsuario = new Usuario(
                nome.trim(),
                emailNormalizado,
                passwordEncoder.encode(senhaPura),
                true
            );
            novoUsuario.adicionarPerfil(perfilObrigatorio);
            return usuarioRepository.save(novoUsuario);
        }

        boolean possuiPerfil = existente.getPerfis().stream()
            .anyMatch(perfil -> perfilObrigatorio.getNome().equals(perfil.getNome()));
        if (!possuiPerfil) {
            existente.adicionarPerfil(perfilObrigatorio);
            return usuarioRepository.save(existente);
        }
        return existente;
    }

    private Categoria garantirCategoria(String nome) {
        return categoriaRepository.findByNomeIgnoreCase(nome)
            .orElseGet(() -> categoriaRepository.save(new Categoria(nome)));
    }

    private Evento garantirEvento(
        String titulo,
        String descricao,
        String local,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusEvento status,
        Categoria categoria,
        Usuario organizador
    ) {
        return eventoRepository.findByTituloIgnoreCase(titulo)
            .orElseGet(() -> eventoRepository.save(new Evento(
                titulo,
                descricao,
                local,
                dataInicio,
                dataFim,
                status,
                categoria,
                organizador
            )));
    }

    private Atividade garantirAtividade(
        Evento evento,
        String titulo,
        TipoAtividade tipo,
        String ministrante,
        LocalDateTime inicio,
        LocalDateTime fim,
        int cargaHoraria,
        int vagas
    ) {
        List<Atividade> existentes = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(evento.getId());
        for (Atividade atividade : existentes) {
            if (atividade.getTitulo().equalsIgnoreCase(titulo)) {
                return atividade;
            }
        }
        return atividadeRepository.save(new Atividade(
            titulo,
            tipo,
            ministrante,
            inicio,
            fim,
            cargaHoraria,
            vagas,
            evento
        ));
    }

    private void garantirInscricao(
        Usuario usuario,
        Atividade atividade,
        LocalDateTime dataHora,
        boolean presente
    ) {
        if (!inscricaoRepository.existsByUsuarioIdAndAtividadeId(usuario.getId(), atividade.getId())) {
            inscricaoRepository.save(new Inscricao(dataHora, presente, usuario, atividade));
        }
    }

    private void higienizarPerfisOrganizadorExclusivo() {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByNomeAsc();
        for (Usuario usuario : usuarios) {
            boolean ehOrganizador = usuario.getPerfis().stream()
                .anyMatch(perfil -> PerfilConstants.ROLE_ORGANIZADOR.equals(perfil.getNome()));
            boolean ehParticipante = usuario.getPerfis().stream()
                .anyMatch(perfil -> PerfilConstants.ROLE_PARTICIPANTE.equals(perfil.getNome()));
            if (ehOrganizador && ehParticipante) {
                usuario.getPerfis().removeIf(perfil -> PerfilConstants.ROLE_PARTICIPANTE.equals(perfil.getNome()));
                usuarioRepository.save(usuario);
            }
        }
    }
}
