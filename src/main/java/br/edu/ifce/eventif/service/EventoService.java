package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.AtividadeComVagasView;
import br.edu.ifce.eventif.dto.EventoDetalheView;
import br.edu.ifce.eventif.dto.EventoForm;
import br.edu.ifce.eventif.dto.EventoResumoView;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.exception.AcessoNegadoPropriedadeException;
import br.edu.ifce.eventif.exception.RecursoNaoEncontradoException;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.CategoriaRepository;
import br.edu.ifce.eventif.repository.EventoRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.ListaEsperaRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de eventos: CRUD do organizador (RF05), ciclo de vida (RF06),
 * validação temporal (RN02), proteção contra IDOR (RS05) e consultas públicas (RF09/RF10).
 */
@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtividadeRepository atividadeRepository;
    private final InscricaoRepository inscricaoRepository;
    private final ListaEsperaRepository listaEsperaRepository;

    public EventoService(
        EventoRepository eventoRepository,
        CategoriaRepository categoriaRepository,
        UsuarioRepository usuarioRepository,
        AtividadeRepository atividadeRepository,
        InscricaoRepository inscricaoRepository,
        ListaEsperaRepository listaEsperaRepository
    ) {
        this.eventoRepository = eventoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.atividadeRepository = atividadeRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.listaEsperaRepository = listaEsperaRepository;
    }

    @Transactional(readOnly = true)
    public Evento buscarEventoDoOrganizadorOu403(Long eventoId, Long organizadorId) {
        Evento evento = eventoRepository.findByIdComDetalhes(eventoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        if (!evento.getOrganizador().getId().equals(organizadorId)) {
            throw new AcessoNegadoPropriedadeException(
                "Você não tem permissão para acessar ou alterar eventos de outro organizador."
            );
        }
        return evento;
    }

    @Transactional(readOnly = true)
    public EventoResumoView buscarResumoEventoDoOrganizadorOu403(Long eventoId, Long organizadorId) {
        return toResumoView(buscarEventoDoOrganizadorOu403(eventoId, organizadorId));
    }

    @Transactional(readOnly = true)
    public List<EventoResumoView> listarPorOrganizador(Long organizadorId) {
        return eventoRepository.findByOrganizadorIdOrderByDataInicioDescIdDesc(organizadorId).stream()
            .map(this::toResumoView)
            .toList();
    }

    @Transactional
    public Evento criar(EventoForm form, Long organizadorId) {
        validarPeriodoEvento(form.getDataInicio(), form.getDataFim());

        Categoria categoria = categoriaRepository.findById(form.getCategoriaId())
            .orElseThrow(() -> new RegraNegocioException("Categoria selecionada não encontrada."));

        Usuario organizador = usuarioRepository.findById(organizadorId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Organizador autenticado não encontrado."));

        Evento evento = new Evento(
            form.getTitulo().trim(),
            form.getDescricao().trim(),
            form.getLocal().trim(),
            form.getDataInicio(),
            form.getDataFim(),
            StatusEvento.RASCUNHO,
            categoria,
            organizador
        );
        return eventoRepository.save(evento);
    }

    @Transactional
    public Evento atualizar(Long eventoId, EventoForm form, Long organizadorId) {
        Evento evento = buscarEventoDoOrganizadorOu403(eventoId, organizadorId);
        if (evento.getStatus() == StatusEvento.ENCERRADO) {
            throw new RegraNegocioException("Eventos encerrados não podem ser editados.");
        }

        validarPeriodoEvento(form.getDataInicio(), form.getDataFim());
        validarAtividadesExistentesDentroDoNovoPeriodo(eventoId, form.getDataInicio(), form.getDataFim());

        Categoria categoria = categoriaRepository.findById(form.getCategoriaId())
            .orElseThrow(() -> new RegraNegocioException("Categoria selecionada não encontrada."));

        evento.setTitulo(form.getTitulo().trim());
        evento.setDescricao(form.getDescricao().trim());
        evento.setLocal(form.getLocal().trim());
        evento.setDataInicio(form.getDataInicio());
        evento.setDataFim(form.getDataFim());
        evento.setCategoria(categoria);

        return eventoRepository.save(evento);
    }

    @Transactional
    public void excluir(Long eventoId, Long organizadorId) {
        Evento evento = buscarEventoDoOrganizadorOu403(eventoId, organizadorId);
        if (evento.getStatus() == StatusEvento.ENCERRADO) {
            throw new RegraNegocioException("Eventos encerrados não podem ser excluídos.");
        }
        if (inscricaoRepository.existsByAtividadeEventoId(eventoId)) {
            throw new RegraNegocioException(
                "Não é possível excluir um evento que já possui participantes inscritos."
            );
        }
        if (listaEsperaRepository.existsByAtividadeEventoId(eventoId)) {
            throw new RegraNegocioException(
                "Não é possível excluir um evento que possui participantes na lista de espera."
            );
        }

        List<Atividade> atividades = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(eventoId);
        if (!atividades.isEmpty()) {
            atividadeRepository.deleteAll(atividades);
        }
        eventoRepository.delete(evento);
    }

    @Transactional
    public void publicar(Long eventoId, Long organizadorId) {
        Evento evento = buscarEventoDoOrganizadorOu403(eventoId, organizadorId);
        if (evento.getStatus() != StatusEvento.RASCUNHO) {
            throw new RegraNegocioException("Somente eventos em rascunho podem ser publicados.");
        }
        evento.setStatus(StatusEvento.PUBLICADO);
        eventoRepository.save(evento);
    }

    @Transactional
    public void encerrar(Long eventoId, Long organizadorId) {
        Evento evento = buscarEventoDoOrganizadorOu403(eventoId, organizadorId);
        if (evento.getStatus() != StatusEvento.PUBLICADO) {
            throw new RegraNegocioException("Somente eventos publicados podem ser encerrados.");
        }
        evento.setStatus(StatusEvento.ENCERRADO);
        eventoRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public Page<EventoResumoView> listarPublicados(Long categoriaId, Pageable pageable) {
        Page<Evento> pagina = categoriaId == null
            ? eventoRepository.findByStatus(StatusEvento.PUBLICADO, pageable)
            : eventoRepository.findByStatusAndCategoriaId(StatusEvento.PUBLICADO, categoriaId, pageable);

        return pagina.map(this::toResumoView);
    }

    @Transactional(readOnly = true)
    public EventoDetalheView buscarDetalhePublico(Long eventoId, Long usuarioAutenticadoIdOpcional) {
        Evento evento = eventoRepository.findByIdComDetalhes(eventoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        if (evento.getStatus() == StatusEvento.RASCUNHO) {
            throw new RecursoNaoEncontradoException("Evento não encontrado.");
        }

        List<Atividade> atividades = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(eventoId);
        Map<Long, Long> contagemPorAtividade = inscricaoRepository.contarInscricoesAgrupadasPorEventoId(eventoId)
            .stream()
            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Set<Long> atividadesInscritas = usuarioAutenticadoIdOpcional == null
            ? Collections.emptySet()
            : inscricaoRepository.findAtividadeIdsInscritasPorUsuarioEEvento(usuarioAutenticadoIdOpcional, eventoId);
        Map<Long, Long> contagemEsperaPorAtividade = listaEsperaRepository.contarFilaAgrupadaPorEventoId(eventoId)
            .stream()
            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Set<Long> atividadesEmEspera = usuarioAutenticadoIdOpcional == null
            ? Collections.emptySet()
            : listaEsperaRepository.findAtividadeIdsEmEsperaPorUsuarioEEvento(usuarioAutenticadoIdOpcional, eventoId);

        List<AtividadeComVagasView> atividadesView = atividades.stream()
            .map(atividade -> {
                long inscritos = contagemPorAtividade.getOrDefault(atividade.getId(), 0L);
                long vagasRestantes = Math.max(0L, atividade.getVagas() - inscritos);
                long totalEspera = contagemEsperaPorAtividade.getOrDefault(atividade.getId(), 0L);
                return new AtividadeComVagasView(
                    atividade.getId(),
                    atividade.getTitulo(),
                    atividade.getTipo(),
                    atividade.getMinistrante(),
                    atividade.getInicio(),
                    atividade.getFim(),
                    atividade.getCargaHoraria(),
                    atividade.getVagas(),
                    inscritos,
                    vagasRestantes,
                    vagasRestantes == 0L,
                    atividadesInscritas.contains(atividade.getId()),
                    atividadesEmEspera.contains(atividade.getId()),
                    totalEspera
                );
            })
            .toList();

        return new EventoDetalheView(
            evento.getId(),
            evento.getTitulo(),
            evento.getDescricao(),
            evento.getLocal(),
            evento.getDataInicio(),
            evento.getDataFim(),
            evento.getStatus(),
            evento.getCategoria().getNome(),
            evento.getOrganizador().getNome(),
            atividadesView
        );
    }

    private void validarPeriodoEvento(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null || dataFim.isBefore(dataInicio)) {
            throw new RegraNegocioException(
                "A data de término do evento deve ser igual ou posterior à data de início."
            );
        }
    }

    private void validarAtividadesExistentesDentroDoNovoPeriodo(
        Long eventoId,
        LocalDate novoInicio,
        LocalDate novoFim
    ) {
        List<Atividade> atividades = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(eventoId);
        for (Atividade atividade : atividades) {
            LocalDate diaInicioAtv = atividade.getInicio().toLocalDate();
            LocalDate diaFimAtv = atividade.getFim().toLocalDate();
            if (diaInicioAtv.isBefore(novoInicio) || diaFimAtv.isAfter(novoFim)) {
                throw new RegraNegocioException(
                    "Não é possível alterar o período do evento porque existem atividades fora do novo intervalo."
                );
            }
        }
    }

    private EventoResumoView toResumoView(Evento evento) {
        return new EventoResumoView(
            evento.getId(),
            evento.getTitulo(),
            evento.getDescricao(),
            evento.getLocal(),
            evento.getDataInicio(),
            evento.getDataFim(),
            evento.getStatus(),
            evento.getCategoria().getId(),
            evento.getCategoria().getNome(),
            evento.getOrganizador().getNome()
        );
    }
}
