package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.dto.CategoriaForm;
import br.edu.ifce.eventif.entity.Categoria;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller administrativo para CRUD completo de categorias (RF03).
 */
@Controller
@RequestMapping("/admin/categorias")
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    public AdminCategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categoriaForm", new CategoriaForm());
        prepararModelo(model, false, null);
        return "admin/categorias";
    }

    @PostMapping
    public String criar(
        @Valid @ModelAttribute("categoriaForm") CategoriaForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararModelo(model, false, null);
            return "admin/categorias";
        }

        try {
            categoriaService.criar(form);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Categoria cadastrada com sucesso.");
            return "redirect:/admin/categorias";
        } catch (RegraNegocioException ex) {
            bindingResult.rejectValue("nome", "categoria.duplicada", ex.getMessage());
            prepararModelo(model, false, null);
            return "admin/categorias";
        }
    }

    @GetMapping("/{id}/editar")
    public String exibirEdicao(@PathVariable Long id, Model model) {
        Categoria categoria = categoriaService.buscarPorId(id);
        model.addAttribute("categoriaForm", new CategoriaForm(categoria.getNome()));
        prepararModelo(model, true, id);
        return "admin/categorias";
    }

    @PostMapping("/{id}")
    public String atualizar(
        @PathVariable Long id,
        @Valid @ModelAttribute("categoriaForm") CategoriaForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        categoriaService.buscarPorId(id);
        if (bindingResult.hasErrors()) {
            prepararModelo(model, true, id);
            return "admin/categorias";
        }

        try {
            categoriaService.atualizar(id, form);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Categoria atualizada com sucesso.");
            return "redirect:/admin/categorias";
        } catch (RegraNegocioException ex) {
            bindingResult.rejectValue("nome", "categoria.duplicada", ex.getMessage());
            prepararModelo(model, true, id);
            return "admin/categorias";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.excluir(id);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Categoria excluída com sucesso.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    private void prepararModelo(Model model, boolean modoEdicao, Long categoriaEdicaoId) {
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("modoEdicao", modoEdicao);
        model.addAttribute("categoriaEdicaoId", categoriaEdicaoId);
    }
}
