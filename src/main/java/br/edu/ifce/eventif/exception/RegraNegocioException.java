package br.edu.ifce.eventif.exception;

/**
 * Exceção de domínio para violações das regras de negócio (RN01–RN07)
 * e restrições de integridade operacional.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
