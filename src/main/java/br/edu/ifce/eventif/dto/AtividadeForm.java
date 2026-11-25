package br.edu.ifce.eventif.dto;

import br.edu.ifce.eventif.enums.TipoAtividade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Formulário DTO para criação e edição de atividade de um evento (RF07).
 */
public class AtividadeForm {

    @NotBlank(message = "O título da atividade é obrigatório.")
    @Size(max = 200, message = "O título da atividade deve ter no máximo 200 caracteres.")
    private String titulo;

    @NotNull(message = "O tipo da atividade é obrigatório.")
    private TipoAtividade tipo;

    @NotBlank(message = "O ministrante é obrigatório.")
    @Size(max = 150, message = "O nome do ministrante deve ter no máximo 150 caracteres.")
    private String ministrante;

    @NotNull(message = "A data e hora de início são obrigatórias.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime inicio;

    @NotNull(message = "A data e hora de término são obrigatórias.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fim;

    @NotNull(message = "A carga horária é obrigatória.")
    @Positive(message = "A carga horária deve ser maior que zero.")
    private Integer cargaHoraria;

    @NotNull(message = "O número de vagas é obrigatório.")
    @Positive(message = "O número de vagas deve ser maior que zero.")
    private Integer vagas;

    public AtividadeForm() {
    }

    public AtividadeForm(
        String titulo,
        TipoAtividade tipo,
        String ministrante,
        LocalDateTime inicio,
        LocalDateTime fim,
        Integer cargaHoraria,
        Integer vagas
    ) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.ministrante = ministrante;
        this.inicio = inicio;
        this.fim = fim;
        this.cargaHoraria = cargaHoraria;
        this.vagas = vagas;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public TipoAtividade getTipo() {
        return tipo;
    }

    public void setTipo(TipoAtividade tipo) {
        this.tipo = tipo;
    }

    public String getMinistrante() {
        return ministrante;
    }

    public void setMinistrante(String ministrante) {
        this.ministrante = ministrante;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public Integer getCargaHoraria() {
        return cargaHoraria;
    }

    public void setCargaHoraria(Integer cargaHoraria) {
        this.cargaHoraria = cargaHoraria;
    }

    public Integer getVagas() {
        return vagas;
    }

    public void setVagas(Integer vagas) {
        this.vagas = vagas;
    }
}
