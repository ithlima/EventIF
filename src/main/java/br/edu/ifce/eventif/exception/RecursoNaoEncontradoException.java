package br.edu.ifce.eventif.exception;

/**
 * Exceção lançada quando um recurso solicitado não existe ou não está disponível publicamente.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
