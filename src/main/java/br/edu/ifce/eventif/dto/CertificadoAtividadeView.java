package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.TipoAtividade;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item de atividade com presença confirmada exibido no certificado (RF14).
 */
public record CertificadoAtividadeView(
    String titulo,
    TipoAtividade tipo,
    BigDecimal cargaHoraria,
    LocalDateTime inicio,
    LocalDateTime fim
) {

    public String getCargaHorariaFormatada() {
        return CargaHorariaFormatter.formatar(cargaHoraria);
    }
}
