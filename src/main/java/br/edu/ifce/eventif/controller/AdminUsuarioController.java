package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller administrativo para listagem de usuários e gestão de ROLE_ORGANIZADOR (RF04).
 */
@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodosParaAdmin());
        return "admin/usuarios";
    }

    @PostMapping("/{id}/conceder-organizador")
    public String concederOrganizador(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.concederPerfilOrganizador(id);
            redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Perfil de organizador concedido com sucesso."
            );
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/remover-organizador")
    public String removerOrganizador(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.removerPerfilOrganizador(id);
            redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Perfil de organizador removido com sucesso."
            );
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}
