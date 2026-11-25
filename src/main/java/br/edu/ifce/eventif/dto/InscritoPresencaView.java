package br.edu.ifce.eventif.dto;

import java.time.LocalDateTime;

/**
 * Projeção imutável de participante inscrito para chamada de presença pelo organizador (RF08).
 */
public record InscritoPresencaView(
    Long inscricaoId,
    Long usuarioId,
    String usuarioNome,
    String usuarioEmail,
    LocalDateTime dataHoraInscricao,
    boolean presente
) {
}
