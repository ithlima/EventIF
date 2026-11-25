package br.edu.ifce.eventif.enums;

/**
 * Tipos permitidos de atividade dentro de um evento acadêmico.
 */
public enum TipoAtividade {
    PALESTRA("Palestra"),
    MINICURSO("Minicurso"),
    OFICINA("Oficina");

    private final String descricao;

    TipoAtividade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
