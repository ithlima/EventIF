package br.edu.ifce.eventif.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Formulário DTO para criação e edição de evento pelo organizador (RF05).
 */
public class EventoForm {

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
    private String titulo;

    @NotBlank(message = "A descrição é obrigatória.")
    private String descricao;

    @NotBlank(message = "O local é obrigatório.")
    @Size(max = 200, message = "O local deve ter no máximo 200 caracteres.")
    private String local;

    @NotNull(message = "A data de início é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @NotNull(message = "A data de término é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    @NotNull(message = "A categoria é obrigatória.")
    private Long categoriaId;

    public EventoForm() {
    }

    public EventoForm(
        String titulo,
        String descricao,
        String local,
        LocalDate dataInicio,
        LocalDate dataFim,
        Long categoriaId
    ) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.local = local;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.categoriaId = categoriaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }
}
