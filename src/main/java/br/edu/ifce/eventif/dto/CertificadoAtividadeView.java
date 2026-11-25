package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.TipoAtividade;
import java.time.LocalDateTime;

/**
 * Item de atividade com presença confirmada exibido no certificado (RF14).
 */
public record CertificadoAtividadeView(
    String titulo,
    TipoAtividade tipo,
    Integer cargaHoraria,
    LocalDateTime inicio,
    LocalDateTime fim
) {
}
