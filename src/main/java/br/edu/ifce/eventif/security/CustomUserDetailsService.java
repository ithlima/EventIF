package br.edu.ifce.eventif.security;

import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega o usuário autenticado a partir do e-mail cadastrado (RS02).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("E-mail não informado.");
        }

        Usuario usuario = usuarioRepository.findWithPerfisByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return CustomUserDetails.fromUsuario(usuario);
    }
}
