package br.edu.ifce.eventif.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Hibernate;

/**
 * Usuário autenticado da plataforma (participante, organizador e/ou administrador).
 */
@Entity
@Table(
    name = "usuarios",
    uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email")
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "senha", nullable = false, length = 255)
    private String senha;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuarios_perfis",
        joinColumns = @JoinColumn(
            name = "usuario_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_usuarios_perfis_usuario")
        ),
        inverseJoinColumns = @JoinColumn(
            name = "perfil_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_usuarios_perfis_perfil")
        )
    )
    private Set<Perfil> perfis = new HashSet<>();

    @OneToMany(mappedBy = "organizador", fetch = FetchType.LAZY)
    private List<Evento> eventosOrganizados = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Inscricao> inscricoes = new ArrayList<>();

    public Usuario() {
    }

    public Usuario(String nome, String email, String senha, boolean ativo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.ativo = ativo;
    }

    public void adicionarPerfil(Perfil perfil) {
        if (perfil != null) {
            this.perfis.add(perfil);
        }
    }

    public void removerPerfil(Perfil perfil) {
        if (perfil != null) {
            this.perfis.remove(perfil);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Set<Perfil> getPerfis() {
        return perfis;
    }

    public void setPerfis(Set<Perfil> perfis) {
        this.perfis = perfis;
    }

    public List<Evento> getEventosOrganizados() {
        return eventosOrganizados;
    }

    public void setEventosOrganizados(List<Evento> eventosOrganizados) {
        this.eventosOrganizados = eventosOrganizados;
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
        Usuario other = (Usuario) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
