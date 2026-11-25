package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.CertificadoAtividadeView;
import br.edu.ifce.eventif.dto.CertificadoView;
import br.edu.ifce.eventif.entity.Atividade;
import br.edu.ifce.eventif.entity.Evento;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.exception.RecursoNaoEncontradoException;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.EventoRepository;
import br.edu.ifce.eventif.repository.InscricaoRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço dinâmico para geração de certificados a partir das presenças registradas (RF14/RN07).
 */
@Service
public class CertificadoService {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscricaoRepository inscricaoRepository;

    public CertificadoService(
        EventoRepository eventoRepository,
        UsuarioRepository usuarioRepository,
        InscricaoRepository inscricaoRepository
    ) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    @Transactional(readOnly = true)
    public CertificadoView emitirCertificado(Long eventoId, Long participanteAutenticadoId) {
        Evento evento = eventoRepository.findByIdComDetalhes(eventoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        Usuario participante = usuarioRepository.findById(participanteAutenticadoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));

        if (evento.getStatus() != StatusEvento.ENCERRADO) {
            throw new RegraNegocioException(
                "O certificado só está disponível após o encerramento do evento."
            );
        }

        List<Inscricao> inscricoesPresentes = inscricaoRepository.findPresentesPorUsuarioIdEEventoId(
            participanteAutenticadoId,
            eventoId
        );

        if (inscricoesPresentes.isEmpty()) {
            throw new RegraNegocioException(
                "O certificado exige presença confirmada em pelo menos uma atividade do evento."
            );
        }

        List<CertificadoAtividadeView> atividadesPresentes = inscricoesPresentes.stream()
            .map(Inscricao::getAtividade)
            .map(this::toCertificadoAtividadeView)
            .toList();

        BigDecimal cargaHorariaTotal = atividadesPresentes.stream()
            .map(CertificadoAtividadeView::cargaHoraria)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CertificadoView(
            evento.getId(),
            participante.getNome(),
            participante.getEmail(),
            evento.getTitulo(),
            evento.getLocal(),
            evento.getDataInicio(),
            evento.getDataFim(),
            cargaHorariaTotal,
            atividadesPresentes
        );
    }

    private CertificadoAtividadeView toCertificadoAtividadeView(Atividade atividade) {
        return new CertificadoAtividadeView(
            atividade.getTitulo(),
            atividade.getTipo(),
            atividade.getCargaHoraria(),
            atividade.getInicio(),
            atividade.getFim()
        );
    }
}
