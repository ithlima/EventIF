package br.edu.ifce.eventif.dto;

/**
 * Projeção imutável de usuário para listagem administrativa sem exposição de hash de senha (RF04).
 */
public record UsuarioResumoView(
    Long id,
    String nome,
    String email,
    boolean ativo,
    boolean admin,
    boolean organizador,
    boolean participante
) {
}
