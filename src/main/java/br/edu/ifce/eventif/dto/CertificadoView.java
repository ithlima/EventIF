package br.edu.ifce.eventif.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Projeção dinâmica de certificado para evento encerrado, sem entidade JPA (RF14/RN07).
 */
public record CertificadoView(
    Long eventoId,
    String participanteNome,
    String participanteEmail,
    String eventoTitulo,
    String eventoLocal,
    LocalDate eventoDataInicio,
    LocalDate eventoDataFim,
    BigDecimal cargaHorariaTotal,
    List<CertificadoAtividadeView> atividadesPresentes
) {

    public String getCargaHorariaTotalFormatada() {
        return CargaHorariaFormatter.formatar(cargaHorariaTotal);
    }
}
