package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.enums.TipoAtividade;
import java.time.LocalDateTime;

/**
 * Projeção imutável de inscrição do participante para a tela "Minhas inscrições" (RF13).
 */
public record MinhaInscricaoView(
    Long inscricaoId,
    Long atividadeId,
    String atividadeTitulo,
    TipoAtividade atividadeTipo,
    LocalDateTime inicio,
    LocalDateTime fim,
    Integer cargaHoraria,
    Long eventoId,
    String eventoTitulo,
    StatusEvento eventoStatus,
    LocalDateTime dataHoraInscricao,
    boolean presente,
    boolean cancelavel,
    boolean certificadoDisponivel
) {
}
