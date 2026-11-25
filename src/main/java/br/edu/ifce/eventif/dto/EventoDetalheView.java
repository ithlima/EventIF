package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.StatusEvento;
import java.time.LocalDate;
import java.util.List;

/**
 * Projeção imutável de detalhes do evento e suas atividades (RF10).
 */
public record EventoDetalheView(
    Long id,
    String titulo,
    String descricao,
    String local,
    LocalDate dataInicio,
    LocalDate dataFim,
    StatusEvento status,
    String categoriaNome,
    String organizadorNome,
    List<AtividadeComVagasView> atividades
) {
}
