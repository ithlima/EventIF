package br.edu.ifce.eventif.security;

import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Representação de usuário autenticado no Spring Security contendo ID e nome da sessão.
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String nome;
    private final String email;
    private final String senha;
    private final boolean ativo;
    private final Set<GrantedAuthority> authorities;

    private CustomUserDetails(
        Long id,
        String nome,
        String email,
        String senha,
        boolean ativo,
        Set<GrantedAuthority> authorities
    ) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.ativo = ativo;
        this.authorities = Set.copyOf(authorities);
    }

    public static CustomUserDetails fromUsuario(Usuario usuario) {
        Set<GrantedAuthority> authorities = usuario.getPerfis().stream()
            .map(Perfil::getNome)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());

        return new CustomUserDetails(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getSenha(),
            usuario.isAtivo(),
            authorities
        );
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
