package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.InscritoPresencaView;
import br.edu.ifce.eventif.dto.MinhaInscricaoView;
import br.edu.ifce.eventif.dto.MinhaListaEsperaView;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.ListaEspera;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.exception.AcessoNegadoPropriedadeException;
import br.edu.ifce.eventif.exception.RecursoNaoEncontradoException;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.ListaEsperaRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de inscrições, cancelamento, lista de espera FIFO, conflito de horário
 * e registro de presença (RF08, RF11–RF13, RN03–RN06, RS05–RS06, EX01, EX02).
 */
@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final AtividadeRepository atividadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtividadeService atividadeService;
    private final Clock clock;

    public InscricaoService(
        InscricaoRepository inscricaoRepository,
        ListaEsperaRepository listaEsperaRepository,
        AtividadeRepository atividadeRepository,
        UsuarioRepository usuarioRepository,
        AtividadeService atividadeService,
        Clock clock
    ) {
        this.inscricaoRepository = inscricaoRepository;
        this.listaEsperaRepository = listaEsperaRepository;
        this.atividadeRepository = atividadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.atividadeService = atividadeService;
        this.clock = clock;
    }

    @Transactional
    public Inscricao inscrever(Long atividadeId, Long participanteAutenticadoId) {
        Atividade atividade = atividadeRepository.findByIdComLock(atividadeId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));

        if (atividade.getEvento().getStatus() != StatusEvento.PUBLICADO) {
            throw new RegraNegocioException(
                "Só é possível se inscrever em atividades de eventos publicados."
            );
        }

        if (inscricaoRepository.existsByUsuarioIdAndAtividadeId(participanteAutenticadoId, atividadeId)) {
            throw new RegraNegocioException("Você já está inscrito nesta atividade.");
        }

        long inscritos = inscricaoRepository.countByAtividadeId(atividadeId);
        if (inscritos >= atividade.getVagas()) {
            throw new RegraNegocioException("Não há vagas disponíveis para esta atividade.");
        }

        validarSemConflitoDeHorario(participanteAutenticadoId, atividade);

        Usuario participante = usuarioRepository.findById(participanteAutenticadoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Participante autenticado não encontrado."));

        Inscricao inscricao = new Inscricao(
            LocalDateTime.now(clock),
            false,
            participante,
            atividade
        );

        try {
            Inscricao salva = inscricaoRepository.saveAndFlush(inscricao);
            listaEsperaRepository.deleteByUsuarioIdAndAtividadeId(participanteAutenticadoId, atividadeId);
            return salva;
        } catch (DataIntegrityViolationException ex) {
            throw new RegraNegocioException("Você já está inscrito nesta atividade.");
        }
    }

    @Transactional
    public ListaEspera entrarNaListaDeEspera(Long atividadeId, Long participanteAutenticadoId) {
        Atividade atividade = atividadeRepository.findByIdComLock(atividadeId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));

        if (atividade.getEvento().getStatus() != StatusEvento.PUBLICADO) {
            throw new RegraNegocioException(
                "Só é possível entrar na lista de espera de eventos publicados."
            );
        }

        if (inscricaoRepository.existsByUsuarioIdAndAtividadeId(participanteAutenticadoId, atividadeId)) {
            throw new RegraNegocioException("Você já está inscrito nesta atividade.");
        }

        if (listaEsperaRepository.existsByUsuarioIdAndAtividadeId(participanteAutenticadoId, atividadeId)) {
            throw new RegraNegocioException("Você já está na lista de espera desta atividade.");
        }

        long inscritos = inscricaoRepository.countByAtividadeId(atividadeId);
        if (inscritos < atividade.getVagas()) {
            throw new RegraNegocioException(
                "Ainda há vagas disponíveis para esta atividade; realize a inscrição direta."
            );
        }

        validarSemConflitoDeHorario(participanteAutenticadoId, atividade);

        Usuario participante = usuarioRepository.findById(participanteAutenticadoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Participante autenticado não encontrado."));

        ListaEspera registro = new ListaEspera(LocalDateTime.now(clock), participante, atividade);
        try {
            return listaEsperaRepository.saveAndFlush(registro);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraNegocioException("Você já está na lista de espera desta atividade.");
        }
    }

    @Transactional
    public void sairDaListaDeEspera(Long listaEsperaId, Long participanteAutenticadoId) {
        ListaEspera item = listaEsperaRepository.findByIdComDetalhes(listaEsperaId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Registro na lista de espera não encontrado."));

        if (!item.getUsuario().getId().equals(participanteAutenticadoId)) {
            throw new AcessoNegadoPropriedadeException(
                "Você não tem permissão para remover outro participante da lista de espera."
            );
        }

        listaEsperaRepository.delete(item);
    }

    @Transactional
    public void cancelar(Long inscricaoId, Long participanteAutenticadoId) {
        Inscricao inscricao = inscricaoRepository.findByIdComDetalhes(inscricaoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Inscrição não encontrada."));

        if (!inscricao.getUsuario().getId().equals(participanteAutenticadoId)) {
            throw new AcessoNegadoPropriedadeException(
                "Você não tem permissão para cancelar a inscrição de outro participante."
            );
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        if (!agora.isBefore(inscricao.getAtividade().getInicio())) {
            throw new RegraNegocioException(
                "O cancelamento só é permitido antes do início da atividade."
            );
        }

        Atividade atividadeBloqueada = atividadeRepository.findByIdComLock(inscricao.getAtividade().getId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));

        inscricaoRepository.delete(inscricao);
        inscricaoRepository.flush();

        if (atividadeBloqueada.getEvento().getStatus() == StatusEvento.PUBLICADO
            && inscricaoRepository.countByAtividadeId(atividadeBloqueada.getId()) < atividadeBloqueada.getVagas()) {
            promoverProximoDaFila(atividadeBloqueada, agora);
        }
    }

    @Transactional(readOnly = true)
    public List<MinhaInscricaoView> listarMinhasInscricoes(Long participanteAutenticadoId) {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Inscricao> inscricoes = inscricaoRepository.findByUsuarioIdComDetalhes(participanteAutenticadoId);

        Set<Long> eventosComPresenca = new HashSet<>();
        for (Inscricao inscricao : inscricoes) {
            if (inscricao.isPresente()) {
                eventosComPresenca.add(inscricao.getAtividade().getEvento().getId());
            }
        }

        return inscricoes.stream()
            .map(inscricao -> {
                Atividade atividade = inscricao.getAtividade();
                var evento = atividade.getEvento();
                boolean cancelavel = agora.isBefore(atividade.getInicio());
                boolean certificadoDisponivel = evento.getStatus() == StatusEvento.ENCERRADO
                    && eventosComPresenca.contains(evento.getId());

                return new MinhaInscricaoView(
                    inscricao.getId(),
                    atividade.getId(),
                    atividade.getTitulo(),
                    atividade.getTipo(),
                    atividade.getInicio(),
                    atividade.getFim(),
                    atividade.getCargaHoraria(),
                    evento.getId(),
                    evento.getTitulo(),
                    evento.getStatus(),
                    inscricao.getDataHora(),
                    inscricao.isPresente(),
                    cancelavel,
                    certificadoDisponivel
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MinhaListaEsperaView> listarMinhaListaDeEspera(Long participanteAutenticadoId) {
        List<ListaEspera> registros = listaEsperaRepository.findByUsuarioIdComDetalhes(participanteAutenticadoId);
        return registros.stream()
            .map(item -> {
                Atividade atividade = item.getAtividade();
                List<ListaEspera> fila = listaEsperaRepository.findFilaOrdenadaPorAtividadeId(atividade.getId());
                int posicao = 1;
                for (int i = 0; i < fila.size(); i++) {
                    if (fila.get(i).getId().equals(item.getId())) {
                        posicao = i + 1;
                        break;
                    }
                }
                return new MinhaListaEsperaView(
                    item.getId(),
                    atividade.getId(),
                    atividade.getTitulo(),
                    atividade.getTipo(),
                    atividade.getInicio(),
                    atividade.getFim(),
                    atividade.getCargaHoraria(),
                    atividade.getEvento().getId(),
                    atividade.getEvento().getTitulo(),
                    item.getDataHora(),
                    posicao
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<InscritoPresencaView> listarInscritosDaAtividade(
        Long eventoId,
        Long atividadeId,
        Long organizadorAutenticadoId
    ) {
        atividadeService.buscarAtividadeDoOrganizadorOu403(eventoId, atividadeId, organizadorAutenticadoId);

        return inscricaoRepository.findByAtividadeIdComUsuario(atividadeId).stream()
            .map(inscricao -> new InscritoPresencaView(
                inscricao.getId(),
                inscricao.getUsuario().getId(),
                inscricao.getUsuario().getNome(),
                inscricao.getUsuario().getEmail(),
                inscricao.getDataHora(),
                inscricao.isPresente()
            ))
            .toList();
    }

    @Transactional
    public void registrarPresencas(
        Long eventoId,
        Long atividadeId,
        Collection<Long> inscricoesPresentesIds,
        Long organizadorAutenticadoId
    ) {
        Atividade atividade = atividadeService.buscarAtividadeDoOrganizadorOu403(
            eventoId,
            atividadeId,
            organizadorAutenticadoId
        );

        if (atividade.getEvento().getStatus() == StatusEvento.RASCUNHO) {
            throw new RegraNegocioException(
                "Não é possível registrar presença em evento que ainda está em rascunho."
            );
        }

        Set<Long> presentesSelecionados = inscricoesPresentesIds == null
            ? Set.of()
            : new HashSet<>(inscricoesPresentesIds);

        List<Inscricao> inscricoes = inscricaoRepository.findByAtividadeIdComUsuario(atividadeId);
        for (Inscricao inscricao : inscricoes) {
            inscricao.setPresente(presentesSelecionados.contains(inscricao.getId()));
        }
        inscricaoRepository.saveAll(inscricoes);
    }

    private void validarSemConflitoDeHorario(Long participanteId, Atividade atividade) {
        List<Inscricao> conflitos = inscricaoRepository.findConflitosDeHorario(
            participanteId,
            atividade.getInicio(),
            atividade.getFim()
        );
        if (!conflitos.isEmpty()) {
            throw new RegraNegocioException(
                "Conflito de horário: você já está inscrito na atividade \""
                    + conflitos.getFirst().getAtividade().getTitulo()
                    + "\" no mesmo período."
            );
        }
    }

    private void promoverProximoDaFila(Atividade atividade, LocalDateTime agora) {
        List<ListaEspera> fila = listaEsperaRepository.findFilaOrdenadaPorAtividadeId(atividade.getId());
        for (ListaEspera candidatoItem : fila) {
            Usuario candidato = candidatoItem.getUsuario();
            boolean jaInscrito = inscricaoRepository.existsByUsuarioIdAndAtividadeId(
                candidato.getId(),
                atividade.getId()
            );
            boolean temConflito = !inscricaoRepository.findConflitosDeHorario(
                candidato.getId(),
                atividade.getInicio(),
                atividade.getFim()
            ).isEmpty();

            if (!jaInscrito && !temConflito) {
                inscricaoRepository.saveAndFlush(new Inscricao(agora, false, candidato, atividade));
                listaEsperaRepository.delete(candidatoItem);
                listaEsperaRepository.flush();
                return;
            }
        }
    }
}
