package br.edu.ifce.eventif.enums;

/**
 * Estados possíveis do ciclo de vida de um evento acadêmico.
 */
public enum StatusEvento {
    RASCUNHO("Rascunho"),
    PUBLICADO("Publicado"),
    ENCERRADO("Encerrado");

    private final String descricao;

    StatusEvento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
