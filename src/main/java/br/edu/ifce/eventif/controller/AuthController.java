package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.dto.CadastroUsuarioForm;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller de autenticação (/login) e cadastro público de participantes (RF01, RF02).
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String exibirCadastro(Model model) {
        if (!model.containsAttribute("cadastroForm")) {
            model.addAttribute("cadastroForm", new CadastroUsuarioForm());
        }
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(
        @Valid @ModelAttribute("cadastroForm") CadastroUsuarioForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/cadastro";
        }

        try {
            usuarioService.cadastrarParticipante(form);
            redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Conta criada com sucesso! Faça login com seu e-mail e senha."
            );
            return "redirect:/login";
        } catch (RegraNegocioException ex) {
            bindingResult.rejectValue("email", "email.duplicado", ex.getMessage());
            return "auth/cadastro";
        }
    }
}
