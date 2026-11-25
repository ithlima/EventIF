package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.dto.CertificadoView;
import br.edu.ifce.eventif.dto.MinhaInscricaoView;
import br.edu.ifce.eventif.dto.MinhaListaEsperaView;
import br.edu.ifce.eventif.entity.Inscricao;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.security.SecurityService;
import br.edu.ifce.eventif.service.AtividadeService;
import br.edu.ifce.eventif.service.CertificadoPdfService;
import br.edu.ifce.eventif.service.CertificadoService;
import br.edu.ifce.eventif.service.InscricaoService;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller das ações do participante autenticado: inscrições, lista de espera,
 * cancelamento e certificados HTML/PDF (RF11–RF14, RS06, EX01, EX03).
 */
@Controller
public class ParticipanteController {

    private final InscricaoService inscricaoService;
    private final CertificadoService certificadoService;
    private final CertificadoPdfService certificadoPdfService;
    private final AtividadeService atividadeService;
    private final SecurityService securityService;

    public ParticipanteController(
        InscricaoService inscricaoService,
        CertificadoService certificadoService,
        CertificadoPdfService certificadoPdfService,
        AtividadeService atividadeService,
        SecurityService securityService
    ) {
        this.inscricaoService = inscricaoService;
        this.certificadoService = certificadoService;
        this.certificadoPdfService = certificadoPdfService;
        this.atividadeService = atividadeService;
        this.securityService = securityService;
    }

    @GetMapping("/inscricoes")
    public String minhasInscricoes(Model model) {
        Long participanteId = securityService.obterUsuarioAutenticadoId();
        List<MinhaInscricaoView> inscricoes = inscricaoService.listarMinhasInscricoes(participanteId);
        List<MinhaListaEsperaView> listaEspera = inscricaoService.listarMinhaListaDeEspera(participanteId);
        model.addAttribute("inscricoes", inscricoes);
        model.addAttribute("listaEspera", listaEspera);
        return "participante/minhas-inscricoes";
    }

    @PostMapping("/inscricoes/atividades/{atividadeId}")
    public String inscrever(
        @PathVariable Long atividadeId,
        RedirectAttributes redirectAttributes
    ) {
        Long participanteId = securityService.obterUsuarioAutenticadoId();
        try {
            Inscricao inscricao = inscricaoService.inscrever(atividadeId, participanteId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Inscrição realizada com sucesso!");
            return "redirect:/eventos/" + inscricao.getAtividade().getEvento().getId();
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
            Long eventoId = atividadeService.buscarEventoIdPorAtividade(atividadeId).orElse(null);
            return eventoId != null ? "redirect:/eventos/" + eventoId : "redirect:/";
        }
    }

    @PostMapping("/inscricoes/atividades/{atividadeId}/espera")
    public String entrarNaListaDeEspera(
        @PathVariable Long atividadeId,
        RedirectAttributes redirectAttributes
    ) {
        Long participanteId = securityService.obterUsuarioAutenticadoId();
        try {
            var registro = inscricaoService.entrarNaListaDeEspera(atividadeId, participanteId);
            redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Você entrou na lista de espera desta atividade."
            );
            return "redirect:/eventos/" + registro.getAtividade().getEvento().getId();
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
            Long eventoId = atividadeService.buscarEventoIdPorAtividade(atividadeId).orElse(null);
            return eventoId != null ? "redirect:/eventos/" + eventoId : "redirect:/";
        }
    }

    @PostMapping("/inscricoes/{inscricaoId}/cancelar")
    public String cancelar(
        @PathVariable Long inscricaoId,
        RedirectAttributes redirectAttributes
    ) {
        Long participanteId = securityService.obterUsuarioAutenticadoId();
        try {
            inscricaoService.cancelar(inscricaoId, participanteId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Inscrição cancelada com sucesso.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/inscricoes";
    }

    @PostMapping("/inscricoes/espera/{listaEsperaId}/cancelar")
    public String sairDaListaDeEspera(
        @PathVariable Long listaEsperaId,
        RedirectAttributes redirectAttributes
    ) {
        Long participanteId = securityService.obterUsuarioAutenticadoId();
        try {
            inscricaoService.sairDaListaDeEspera(listaEsperaId, participanteId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Você saiu da lista de espera.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
        }
        return "redirect:/inscricoes";
    }

    @GetMapping("/certificados/eventos/{eventoId}")
    public String certificado(@PathVariable Long eventoId, Model model) {
        Long participanteId = securityService.obterUsuarioAutenticadoId();
        CertificadoView certificado = certificadoService.emitirCertificado(eventoId, participanteId);
        model.addAttribute("certificado", certificado);
        return "participante/certificado";
    }

    @GetMapping("/certificados/eventos/{eventoId}/pdf")
    public ResponseEntity<byte[]> certificadoPdf(@PathVariable Long eventoId) {
        Long participanteId = securityService.obterUsuarioAutenticadoId();
        CertificadoView certificado = certificadoService.emitirCertificado(eventoId, participanteId);
        byte[] pdfBytes = certificadoPdfService.gerarPdf(certificado);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("certificado-evento-" + eventoId + ".pdf")
                .build()
        );
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
