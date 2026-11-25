package br.edu.ifce.eventif.entity;

import br.edu.ifce.eventif.enums.StatusEvento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.Hibernate;

/**
 * Evento acadêmico divulgado na plataforma EventIF.
 *
 * <p>Decisão arquitetural (ADR-04): {@code dataInicio} e {@code dataFim} utilizam
 * {@link java.time.LocalDate} para representar os dias de calendário do evento
 * (permitindo eventos de 1 dia quando {@code dataFim.equals(dataInicio)} na RN02),
 * enquanto as atividades definem seus horários exatos com {@link java.time.LocalDateTime}.</p>
 */
@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    /**
     * Coluna física {@code local_evento}: {@code LOCAL} é palavra reservada do MySQL 8.x
     * e quebraria o DDL/consultas em execução real (ADR-14). O atributo Java permanece {@code local}.
     */
    @Column(name = "local_evento", nullable = false, length = 200)
    private String local;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusEvento status = StatusEvento.RASCUNHO;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "categoria_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_eventos_categoria")
    )
    private Categoria categoria;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "organizador_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_eventos_organizador")
    )
    private Usuario organizador;

    @OneToMany(mappedBy = "evento", fetch = FetchType.LAZY)
    private List<Atividade> atividades = new ArrayList<>();

    public Evento() {
    }

    public Evento(
        String titulo,
        String descricao,
        String local,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusEvento status,
        Categoria categoria,
        Usuario organizador
    ) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.local = local;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status != null ? status : StatusEvento.RASCUNHO;
        this.categoria = categoria;
        this.organizador = organizador;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public StatusEvento getStatus() {
        return status;
    }

    public void setStatus(StatusEvento status) {
        this.status = status;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Usuario getOrganizador() {
        return organizador;
    }

    public void setOrganizador(Usuario organizador) {
        this.organizador = organizador;
    }

    public List<Atividade> getAtividades() {
        return atividades;
    }

    public void setAtividades(List<Atividade> atividades) {
        this.atividades = atividades;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Evento other = (Evento) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
