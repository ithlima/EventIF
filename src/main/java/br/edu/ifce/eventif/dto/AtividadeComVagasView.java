package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.TipoAtividade;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projeção imutável de atividade com cálculo de vagas ocupadas, restantes e fila de espera (RF10, EX01).
 */
public record AtividadeComVagasView(
    Long id,
    String titulo,
    TipoAtividade tipo,
    String ministrante,
    LocalDateTime inicio,
    LocalDateTime fim,
    BigDecimal cargaHoraria,
    Integer vagas,
    long inscritos,
    long vagasRestantes,
    boolean esgotada,
    boolean inscritoUsuarioAtual,
    boolean emListaEsperaUsuarioAtual,
    long totalListaEspera
) {

    public AtividadeComVagasView(
        Long id,
        String titulo,
        TipoAtividade tipo,
        String ministrante,
        LocalDateTime inicio,
        LocalDateTime fim,
        BigDecimal cargaHoraria,
        Integer vagas,
        long inscritos,
        long vagasRestantes,
        boolean esgotada,
        boolean inscritoUsuarioAtual
    ) {
        this(
            id,
            titulo,
            tipo,
            ministrante,
            inicio,
            fim,
            cargaHoraria,
            vagas,
            inscritos,
            vagasRestantes,
            esgotada,
            inscritoUsuarioAtual,
            false,
            0L
        );
    }

    public String getCargaHorariaFormatada() {
        return CargaHorariaFormatter.formatar(cargaHoraria);
    }
}
