package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.dto.EventoForm;
import br.edu.ifce.eventif.dto.EventoResumoView;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.security.SecurityService;
import br.edu.ifce.eventif.service.CategoriaService;
import br.edu.ifce.eventif.service.EventoService;
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
 * Controller do organizador para gestão dos próprios eventos (RF05, RF06, RS05).
 */
@Controller
@RequestMapping("/organizador/eventos")
public class OrganizadorEventoController {

    private final EventoService eventoService;
    private final CategoriaService categoriaService;
    private final SecurityService securityService;

    public OrganizadorEventoController(
        EventoService eventoService,
        CategoriaService categoriaService,
        SecurityService securityService
    ) {
        this.eventoService = eventoService;
        this.categoriaService = categoriaService;
        this.securityService = securityService;
    }

    @GetMapping
    public String meusEventos(Model model) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        model.addAttribute("eventos", eventoService.listarPorOrganizador(organizadorId));
        return "organizador/eventos";
    }

    @GetMapping("/novo")
    public String exibirFormularioCriacao(Model model) {
        model.addAttribute("eventoForm", new EventoForm());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("modoEdicao", false);
        return "organizador/evento-form";
    }

    @PostMapping
    public String criar(
        @Valid @ModelAttribute("eventoForm") EventoForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        if (bindingResult.hasErrors()) {
            prepararModeloFormulario(model, null, false);
            return "organizador/evento-form";
        }

        try {
            eventoService.criar(form, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Evento criado como rascunho com sucesso.");
            return "redirect:/organizador/eventos";
        } catch (RegraNegocioException ex) {
            model.addAttribute("erroNegocio", ex.getMessage());
            prepararModeloFormulario(model, null, false);
            return "organizador/evento-form";
        }
    }

    @GetMapping("/{id}/editar")
    public String exibirFormularioEdicao(
        @PathVariable Long id,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        EventoResumoView evento = eventoService.buscarResumoEventoDoOrganizadorOu403(id, organizadorId);

        if (evento.status() == StatusEvento.ENCERRADO) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Eventos encerrados não podem ser editados.");
            return "redirect:/organizador/eventos";
        }

        EventoForm form = new EventoForm(
            evento.titulo(),
            evento.descricao(),
            evento.local(),
            evento.dataInicio(),
            evento.dataFim(),
            evento.categoriaId()
        );
        model.addAttribute("eventoForm", form);
        prepararModeloFormulario(model, id, true);
        return "organizador/evento-form";
    }

    @PostMapping("/{id}")
    public String atualizar(
        @PathVariable Long id,
        @Valid @ModelAttribute("eventoForm") EventoForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        eventoService.buscarResumoEventoDoOrganizadorOu403(id, organizadorId);

        if (bindingResult.hasErrors()) {
            prepararModeloFormulario(model, id, true);
            return "organizador/evento-form";
        }

        try {
            eventoService.atualizar(id, form, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Evento atualizado com sucesso.");
            return "redirect:/organizador/eventos";
        } catch (RegraNegocioException ex) {
            model.addAttribute("erroNegocio", ex.getMessage());
            prepararModeloFormulario(model, id, true);
            return "organizador/evento-form";
        }
    }

    @PostMapping("/{id}/publicar")
    public String publicar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        try {
            eventoService.publicar(id, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Evento publicado com sucesso.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/organizador/eventos";
    }

    @PostMapping("/{id}/encerrar")
    public String encerrar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        try {
            eventoService.encerrar(id, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Evento encerrado com sucesso.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/organizador/eventos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        try {
            eventoService.excluir(id, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Evento excluído com sucesso.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/organizador/eventos";
    }

    private void prepararModeloFormulario(Model model, Long eventoId, boolean modoEdicao) {
        model.addAttribute("eventoId", eventoId);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("modoEdicao", modoEdicao);
    }
}
