package br.edu.ifce.eventif.exception;

/**
 * Exceção lançada quando um usuário tenta acessar ou alterar registro pertencente a outro usuário (RS05/RS06).
 */
public class AcessoNegadoPropriedadeException extends RuntimeException {

    public AcessoNegadoPropriedadeException(String message) {
        super(message);
    }
}
