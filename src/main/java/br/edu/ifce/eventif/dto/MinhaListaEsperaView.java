package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.TipoAtividade;
import java.math.BigDecimal;
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
    BigDecimal cargaHoraria,
    Long eventoId,
    String eventoTitulo,
    LocalDateTime dataHoraEntrada,
    int posicaoFila
) {

    public String getCargaHorariaFormatada() {
        return CargaHorariaFormatter.formatar(cargaHoraria);
    }
}
