package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.StatusEvento;
import java.time.LocalDate;

/**
 * Projeção imutável de resumo de evento para listagens públicas e do organizador.
 */
public record EventoResumoView(
    Long id,
    String titulo,
    String descricao,
    String local,
    LocalDate dataInicio,
    LocalDate dataFim,
    StatusEvento status,
    Long categoriaId,
    String categoriaNome,
    String organizadorNome
) {
}
