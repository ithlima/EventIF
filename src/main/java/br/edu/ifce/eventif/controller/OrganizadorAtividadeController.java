package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.dto.AtividadeComVagasView;
import br.edu.ifce.eventif.dto.AtividadeForm;
import br.edu.ifce.eventif.dto.EventoResumoView;
import br.edu.ifce.eventif.enums.StatusEvento;
import br.edu.ifce.eventif.enums.TipoAtividade;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.security.SecurityService;
import br.edu.ifce.eventif.service.AtividadeService;
import br.edu.ifce.eventif.service.EventoService;
import br.edu.ifce.eventif.service.InscricaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller do organizador para CRUD de atividades e registro de presença (RF07, RF08, RS05).
 */
@Controller
@RequestMapping("/organizador/eventos/{eventoId}/atividades")
public class OrganizadorAtividadeController {

    private final EventoService eventoService;
    private final AtividadeService atividadeService;
    private final InscricaoService inscricaoService;
    private final SecurityService securityService;

    public OrganizadorAtividadeController(
        EventoService eventoService,
        AtividadeService atividadeService,
        InscricaoService inscricaoService,
        SecurityService securityService
    ) {
        this.eventoService = eventoService;
        this.atividadeService = atividadeService;
        this.inscricaoService = inscricaoService;
        this.securityService = securityService;
    }

    @GetMapping
    public String listar(@PathVariable Long eventoId, Model model) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        EventoResumoView evento = eventoService.buscarResumoEventoDoOrganizadorOu403(eventoId, organizadorId);

        model.addAttribute("atividadeForm", new AtividadeForm());
        prepararModeloAtividades(model, evento, organizadorId, false, null);
        return "organizador/atividades";
    }

    @PostMapping
    public String criar(
        @PathVariable Long eventoId,
        @Valid @ModelAttribute("atividadeForm") AtividadeForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        EventoResumoView evento = eventoService.buscarResumoEventoDoOrganizadorOu403(eventoId, organizadorId);

        if (bindingResult.hasErrors()) {
            prepararModeloAtividades(model, evento, organizadorId, false, null);
            return "organizador/atividades";
        }

        try {
            atividadeService.criar(eventoId, form, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Atividade criada com sucesso.");
            return "redirect:/organizador/eventos/" + eventoId + "/atividades";
        } catch (RegraNegocioException ex) {
            model.addAttribute("erroNegocio", ex.getMessage());
            prepararModeloAtividades(model, evento, organizadorId, false, null);
            return "organizador/atividades";
        }
    }

    @GetMapping("/{atividadeId}/editar")
    public String exibirEdicao(
        @PathVariable Long eventoId,
        @PathVariable Long atividadeId,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        EventoResumoView evento = eventoService.buscarResumoEventoDoOrganizadorOu403(eventoId, organizadorId);
        AtividadeComVagasView atv = atividadeService.buscarResumoAtividadeDoOrganizadorOu403(
            eventoId, atividadeId, organizadorId
        );

        if (evento.status() == StatusEvento.ENCERRADO) {
            redirectAttributes.addFlashAttribute(
                "mensagemErro",
                "Não é possível editar atividades de um evento encerrado."
            );
            return "redirect:/organizador/eventos/" + eventoId + "/atividades";
        }

        AtividadeForm form = new AtividadeForm(
            atv.titulo(),
            atv.tipo(),
            atv.ministrante(),
            atv.inicio(),
            atv.fim(),
            atv.cargaHoraria(),
            atv.vagas()
        );
        model.addAttribute("atividadeForm", form);
        prepararModeloAtividades(model, evento, organizadorId, true, atividadeId);
        return "organizador/atividades";
    }

    @PostMapping("/{atividadeId}")
    public String atualizar(
        @PathVariable Long eventoId,
        @PathVariable Long atividadeId,
        @Valid @ModelAttribute("atividadeForm") AtividadeForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        EventoResumoView evento = eventoService.buscarResumoEventoDoOrganizadorOu403(eventoId, organizadorId);
        atividadeService.buscarResumoAtividadeDoOrganizadorOu403(eventoId, atividadeId, organizadorId);

        if (bindingResult.hasErrors()) {
            prepararModeloAtividades(model, evento, organizadorId, true, atividadeId);
            return "organizador/atividades";
        }

        try {
            atividadeService.atualizar(eventoId, atividadeId, form, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Atividade atualizada com sucesso.");
            return "redirect:/organizador/eventos/" + eventoId + "/atividades";
        } catch (RegraNegocioException ex) {
            model.addAttribute("erroNegocio", ex.getMessage());
            prepararModeloAtividades(model, evento, organizadorId, true, atividadeId);
            return "organizador/atividades";
        }
    }

    @PostMapping("/{atividadeId}/excluir")
    public String excluir(
        @PathVariable Long eventoId,
        @PathVariable Long atividadeId,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        try {
            atividadeService.excluir(eventoId, atividadeId, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Atividade excluída com sucesso.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/organizador/eventos/" + eventoId + "/atividades";
    }

    @GetMapping("/{atividadeId}/inscritos")
    public String listarInscritos(
        @PathVariable Long eventoId,
        @PathVariable Long atividadeId,
        Model model
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        EventoResumoView evento = eventoService.buscarResumoEventoDoOrganizadorOu403(eventoId, organizadorId);
        AtividadeComVagasView atividade = atividadeService.buscarResumoAtividadeDoOrganizadorOu403(
            eventoId, atividadeId, organizadorId
        );

        model.addAttribute("evento", evento);
        model.addAttribute("atividade", atividade);
        model.addAttribute(
            "inscritos",
            inscricaoService.listarInscritosDaAtividade(eventoId, atividadeId, organizadorId)
        );
        return "organizador/inscritos";
    }

    @PostMapping("/{atividadeId}/inscritos")
    public String registrarPresencas(
        @PathVariable Long eventoId,
        @PathVariable Long atividadeId,
        @RequestParam(name = "presentesIds", required = false) List<Long> presentesIds,
        RedirectAttributes redirectAttributes
    ) {
        Long organizadorId = securityService.obterUsuarioAutenticadoId();
        try {
            inscricaoService.registrarPresencas(eventoId, atividadeId, presentesIds, organizadorId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Presenças atualizadas com sucesso.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/organizador/eventos/" + eventoId + "/atividades/" + atividadeId + "/inscritos";
    }

    private void prepararModeloAtividades(
        Model model,
        EventoResumoView evento,
        Long organizadorId,
        boolean modoEdicao,
        Long atividadeEdicaoId
    ) {
        model.addAttribute("evento", evento);
        model.addAttribute("atividades", atividadeService.listarPorEventoDoOrganizador(evento.id(), organizadorId));
        model.addAttribute("tiposAtividade", TipoAtividade.values());
        model.addAttribute("modoEdicao", modoEdicao);
        model.addAttribute("atividadeEdicaoId", atividadeEdicaoId);
    }
}
