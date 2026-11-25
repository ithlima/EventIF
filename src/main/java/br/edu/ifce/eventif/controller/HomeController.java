package br.edu.ifce.eventif.controller;

import br.edu.ifce.eventif.dto.EventoDetalheView;
import br.edu.ifce.eventif.dto.EventoResumoView;
import br.edu.ifce.eventif.security.SecurityService;
import br.edu.ifce.eventif.service.CategoriaService;
import br.edu.ifce.eventif.service.EventoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller público da página inicial com eventos publicados, paginação,
 * filtro por categoria e detalhes de evento (RF09, RF10).
 */
@Controller
public class HomeController {

    private static final int TAMANHO_PAGINA_PADRAO = 6;
    private static final int TAMANHO_PAGINA_MAXIMO = 24;

    private final EventoService eventoService;
    private final CategoriaService categoriaService;
    private final SecurityService securityService;

    public HomeController(
        EventoService eventoService,
        CategoriaService categoriaService,
        SecurityService securityService
    ) {
        this.eventoService = eventoService;
        this.categoriaService = categoriaService;
        this.securityService = securityService;
    }

    @GetMapping({"/", "/eventos"})
    public String index(
        @RequestParam(required = false) Long categoriaId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "6") int size,
        Model model
    ) {
        int paginaSegura = Math.max(0, page);
        int tamanhoSeguro = (size <= 0 || size > TAMANHO_PAGINA_MAXIMO) ? TAMANHO_PAGINA_PADRAO : size;
        Pageable pageable = PageRequest.of(
            paginaSegura,
            tamanhoSeguro,
            Sort.by(Sort.Order.asc("dataInicio"), Sort.Order.asc("id"))
        );

        Page<EventoResumoView> paginaEventos = eventoService.listarPublicados(categoriaId, pageable);

        model.addAttribute("tituloSistema", "EventIF");
        model.addAttribute("subtitulo", "Sistema de Gestão de Eventos Acadêmicos");
        model.addAttribute("paginaEventos", paginaEventos);
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("categoriaSelecionada", categoriaId);
        return "publico/index";
    }

    @GetMapping("/eventos/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        Long usuarioId = securityService.obterUsuarioAutenticadoIdOpcional().orElse(null);
        EventoDetalheView evento = eventoService.buscarDetalhePublico(id, usuarioId);
        model.addAttribute("evento", evento);
        return "publico/evento-detalhe";
    }
}
