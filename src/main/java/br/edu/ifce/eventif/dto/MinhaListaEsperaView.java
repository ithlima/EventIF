package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.TipoAtividade;
import java.time.LocalDateTime;

/**
 * Projeção imutável de entrada do participante na lista de espera (EX01).
 */
public record MinhaListaEsperaView(
    Long listaEsperaId,
    Long atividadeId,
    String atividadeTitulo,
    TipoAtividade atividadeTipo,
    LocalDateTime inicio,
    LocalDateTime fim,
    Integer cargaHoraria,
    Long eventoId,
    String eventoTitulo,
    LocalDateTime dataHoraEntrada,
    int posicaoFila
) {
}
