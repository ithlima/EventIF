package br.edu.ifce.eventif.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Tratamento centralizado de exceções da camada de serviço com status HTTP e páginas amigáveis.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AcessoNegadoPropriedadeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAcessoNegado(AcessoNegadoPropriedadeException ex, Model model) {
        model.addAttribute("detalheErro", ex.getMessage());
        return "error/403";
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex, Model model) {
        model.addAttribute("detalheErro", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleRegraNegocio(RegraNegocioException ex, Model model) {
        model.addAttribute("detalheErro", ex.getMessage());
        return "error/regra-negocio";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflitoIntegridade(DataIntegrityViolationException ex, Model model) {
        model.addAttribute(
            "detalheErro",
            "Não foi possível concluir a operação devido a conflito de dados ou registro vinculado."
        );
        return "error/regra-negocio";
    }
}
