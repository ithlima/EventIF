package br.edu.ifce.eventif.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.Hibernate;

/**
 * Inscrição de um usuário participante em uma atividade de evento acadêmico.
 */
@Entity
@Table(
    name = "inscricoes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inscricoes_usuario_atividade",
        columnNames = {"usuario_id", "atividade_id"}
    )
)
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "presente", nullable = false)
    private boolean presente = false;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "usuario_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_inscricoes_usuario")
    )
    private Usuario usuario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "atividade_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_inscricoes_atividade")
    )
    private Atividade atividade;

    public Inscricao() {
    }

    public Inscricao(LocalDateTime dataHora, boolean presente, Usuario usuario, Atividade atividade) {
        this.dataHora = dataHora;
        this.presente = presente;
        this.usuario = usuario;
        this.atividade = atividade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public boolean isPresente() {
        return presente;
    }

    public void setPresente(boolean presente) {
        this.presente = presente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Atividade getAtividade() {
        return atividade;
    }

    public void setAtividade(Atividade atividade) {
        this.atividade = atividade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Inscricao other = (Inscricao) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
