package br.edu.ifce.eventif.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Hibernate;

/**
 * Perfil de acesso do usuário no sistema (ROLE_PARTICIPANTE, ROLE_ORGANIZADOR, ROLE_ADMIN).
 */
@Entity
@Table(
    name = "perfis",
    uniqueConstraints = @UniqueConstraint(name = "uk_perfis_nome", columnNames = "nome")
)
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 50)
    private String nome;

    @ManyToMany(mappedBy = "perfis", fetch = FetchType.LAZY)
    private Set<Usuario> usuarios = new HashSet<>();

    public Perfil() {
    }

    public Perfil(String nome) {
        this.nome = nome;
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

    public Set<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(Set<Usuario> usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Perfil other = (Perfil) o;
        return nome != null && Objects.equals(nome, other.nome);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nome);
    }
}
