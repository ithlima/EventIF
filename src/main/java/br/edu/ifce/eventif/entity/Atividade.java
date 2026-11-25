package br.edu.ifce.eventif.entity;

import br.edu.ifce.eventif.enums.TipoAtividade;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.Hibernate;

/**
 * Atividade vinculada a um evento acadêmico (palestra, minicurso ou oficina).
 */
@Entity
@Table(name = "atividades")
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAtividade tipo;

    @Column(name = "ministrante", nullable = false, length = 150)
    private String ministrante;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fim", nullable = false)
    private LocalDateTime fim;

    @Column(name = "carga_horaria", nullable = false, precision = 6, scale = 2)
    private BigDecimal cargaHoraria;

    @Column(name = "vagas", nullable = false)
    private Integer vagas;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "evento_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_atividades_evento")
    )
    private Evento evento;

    @OneToMany(mappedBy = "atividade", fetch = FetchType.LAZY)
    private List<Inscricao> inscricoes = new ArrayList<>();

    public Atividade() {
    }

    public Atividade(
        String titulo,
        TipoAtividade tipo,
        String ministrante,
        LocalDateTime inicio,
        LocalDateTime fim,
        BigDecimal cargaHoraria,
        Integer vagas,
        Evento evento
    ) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.ministrante = ministrante;
        this.inicio = inicio;
        this.fim = fim;
        this.cargaHoraria = cargaHoraria;
        this.vagas = vagas;
        this.evento = evento;
    }

    public Atividade(
        String titulo,
        TipoAtividade tipo,
        String ministrante,
        LocalDateTime inicio,
        LocalDateTime fim,
        int cargaHoraria,
        Integer vagas,
        Evento evento
    ) {
        this(titulo, tipo, ministrante, inicio, fim, BigDecimal.valueOf(cargaHoraria), vagas, evento);
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

    public BigDecimal getCargaHoraria() {
        return cargaHoraria;
    }

    public void setCargaHoraria(BigDecimal cargaHoraria) {
        this.cargaHoraria = cargaHoraria;
    }

    public Integer getVagas() {
        return vagas;
    }

    public void setVagas(Integer vagas) {
        this.vagas = vagas;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public List<Inscricao> getInscricoes() {
        return inscricoes;
    }

    public void setInscricoes(List<Inscricao> inscricoes) {
        this.inscricoes = inscricoes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Atividade other = (Atividade) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
