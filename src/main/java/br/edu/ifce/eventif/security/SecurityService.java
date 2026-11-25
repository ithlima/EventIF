package br.edu.ifce.eventif.security;

import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Componente auxiliar para obter o usuário autenticado diretamente da sessão Spring Security (RS06).
 */
@Component
public class SecurityService {

    private final UsuarioRepository usuarioRepository;

    public SecurityService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<CustomUserDetails> obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return Optional.of(customUserDetails);
        }
        return Optional.empty();
    }

    public Optional<Long> obterUsuarioAutenticadoIdOpcional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return Optional.of(customUserDetails.getId());
        }
        if (principal instanceof UserDetails userDetails) {
            return usuarioRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .map(Usuario::getId);
        }
        return Optional.empty();
    }

    public Long obterUsuarioAutenticadoId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Usuário não autenticado.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }
        if (principal instanceof UserDetails userDetails) {
            return usuarioRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .map(Usuario::getId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Usuário autenticado não encontrado."));
        }
        throw new AuthenticationCredentialsNotFoundException("Principal de autenticação inválido.");
    }
}
