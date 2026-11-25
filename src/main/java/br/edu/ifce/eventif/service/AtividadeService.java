package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.AtividadeComVagasView;
import br.edu.ifce.eventif.dto.AtividadeForm;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.exception.AcessoNegadoPropriedadeException;
import br.edu.ifce.eventif.exception.RecursoNaoEncontradoException;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.AtividadeRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.ListaEsperaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de gerenciamento de atividades pelo organizador (RF07, RN02, RS05).
 */
@Service
public class AtividadeService {

    private final AtividadeRepository atividadeRepository;
    private final InscricaoRepository inscricaoRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final EventoService eventoService;

    public AtividadeService(
        AtividadeRepository atividadeRepository,
        InscricaoRepository inscricaoRepository,
        ListaEsperaRepository listaEsperaRepository,
        EventoService eventoService
    ) {
        this.atividadeRepository = atividadeRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.listaEsperaRepository = listaEsperaRepository;
        this.eventoService = eventoService;
    }

    @Transactional(readOnly = true)
    public Optional<Long> buscarEventoIdPorAtividade(Long atividadeId) {
        return atividadeRepository.findByIdComEventoEOrganizador(atividadeId)
            .map(atividade -> atividade.getEvento().getId());
    }

    @Transactional(readOnly = true)
    public Atividade buscarAtividadeDoOrganizadorOu403(Long eventoId, Long atividadeId, Long organizadorId) {
        eventoService.buscarEventoDoOrganizadorOu403(eventoId, organizadorId);

        Atividade atividade = atividadeRepository.findByIdComEventoEOrganizador(atividadeId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));

        if (!atividade.getEvento().getOrganizador().getId().equals(organizadorId)) {
            throw new AcessoNegadoPropriedadeException(
                "Você não tem permissão para acessar ou alterar atividades de outro organizador."
            );
        }

        if (!atividade.getEvento().getId().equals(eventoId)) {
            throw new RecursoNaoEncontradoException("Atividade não pertence ao evento informado.");
        }

        return atividade;
    }

    @Transactional(readOnly = true)
    public AtividadeComVagasView buscarResumoAtividadeDoOrganizadorOu403(
        Long eventoId,
        Long atividadeId,
        Long organizadorId
    ) {
        Atividade atividade = buscarAtividadeDoOrganizadorOu403(eventoId, atividadeId, organizadorId);
        long inscritos = inscricaoRepository.countByAtividadeId(atividadeId);
        long vagasRestantes = Math.max(0L, atividade.getVagas() - inscritos);
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
            false
        );
    }

    @Transactional(readOnly = true)
    public List<AtividadeComVagasView> listarPorEventoDoOrganizador(Long eventoId, Long organizadorId) {
        eventoService.buscarEventoDoOrganizadorOu403(eventoId, organizadorId);

        List<Atividade> atividades = atividadeRepository.findByEventoIdOrderByInicioAscIdAsc(eventoId);
        Map<Long, Long> contagemPorAtividade = inscricaoRepository.contarInscricoesAgrupadasPorEventoId(eventoId)
            .stream()
            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return atividades.stream()
            .map(atividade -> {
                long inscritos = contagemPorAtividade.getOrDefault(atividade.getId(), 0L);
                long vagasRestantes = Math.max(0L, atividade.getVagas() - inscritos);
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
                    false
                );
            })
            .toList();
    }

    @Transactional
    public Atividade criar(Long eventoId, AtividadeForm form, Long organizadorId) {
        Evento evento = eventoService.buscarEventoDoOrganizadorOu403(eventoId, organizadorId);
        if (evento.getStatus() == StatusEvento.ENCERRADO) {
            throw new RegraNegocioException("Não é possível adicionar atividades a um evento encerrado.");
        }

        validarDadosAtividade(form, evento);

        Atividade atividade = new Atividade(
            form.getTitulo().trim(),
            form.getTipo(),
            form.getMinistrante().trim(),
            form.getInicio(),
            form.getFim(),
            form.getCargaHoraria(),
            form.getVagas(),
            evento
        );
        return atividadeRepository.save(atividade);
    }

    @Transactional
    public Atividade atualizar(Long eventoId, Long atividadeId, AtividadeForm form, Long organizadorId) {
        Atividade atividade = buscarAtividadeDoOrganizadorOu403(eventoId, atividadeId, organizadorId);
        Evento evento = atividade.getEvento();
        if (evento.getStatus() == StatusEvento.ENCERRADO) {
            throw new RegraNegocioException("Não é possível editar atividades de um evento encerrado.");
        }

        validarDadosAtividade(form, evento);

        long inscritosAtuais = inscricaoRepository.countByAtividadeId(atividadeId);
        if (form.getVagas() < inscritosAtuais) {
            throw new RegraNegocioException(
                "O número de vagas não pode ser menor que o total de participantes já inscritos ("
                    + inscritosAtuais + ")."
            );
        }

        atividade.setTitulo(form.getTitulo().trim());
        atividade.setTipo(form.getTipo());
        atividade.setMinistrante(form.getMinistrante().trim());
        atividade.setInicio(form.getInicio());
        atividade.setFim(form.getFim());
        atividade.setCargaHoraria(form.getCargaHoraria());
        atividade.setVagas(form.getVagas());

        return atividadeRepository.save(atividade);
    }

    @Transactional
    public void excluir(Long eventoId, Long atividadeId, Long organizadorId) {
        Atividade atividade = buscarAtividadeDoOrganizadorOu403(eventoId, atividadeId, organizadorId);
        if (atividade.getEvento().getStatus() == StatusEvento.ENCERRADO) {
            throw new RegraNegocioException("Não é possível excluir atividades de um evento encerrado.");
        }
        if (inscricaoRepository.existsByAtividadeId(atividadeId)) {
            throw new RegraNegocioException(
                "Não é possível excluir uma atividade que já possui participantes inscritos."
            );
        }
        if (listaEsperaRepository.existsByAtividadeId(atividadeId)) {
            throw new RegraNegocioException(
                "Não é possível excluir uma atividade que possui participantes na lista de espera."
            );
        }
        atividadeRepository.delete(atividade);
    }

    private void validarDadosAtividade(AtividadeForm form, Evento evento) {
        LocalDateTime inicio = form.getInicio();
        LocalDateTime fim = form.getFim();

        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new RegraNegocioException("O horário de término da atividade deve ser posterior ao início.");
        }

        LocalDate diaInicioAtividade = inicio.toLocalDate();
        LocalDate diaFimAtividade = fim.toLocalDate();
        if (diaInicioAtividade.isBefore(evento.getDataInicio()) || diaFimAtividade.isAfter(evento.getDataFim())) {
            throw new RegraNegocioException(
                "A atividade deve ocorrer inteiramente dentro do período do evento."
            );
        }

        if (form.getCargaHoraria() == null || form.getCargaHoraria() <= 0) {
            throw new RegraNegocioException("A carga horária da atividade deve ser maior que zero.");
        }

        if (form.getVagas() == null || form.getVagas() <= 0) {
            throw new RegraNegocioException("O número de vagas da atividade deve ser maior que zero.");
        }
    }
}
