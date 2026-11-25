package br.edu.ifce.eventif.service;

import br.edu.ifce.eventif.dto.CadastroUsuarioForm;
import br.edu.ifce.eventif.dto.UsuarioResumoView;
import br.edu.ifce.eventif.entity.Perfil;
import br.edu.ifce.eventif.entity.Usuario;
import br.edu.ifce.eventif.exception.RecursoNaoEncontradoException;
import br.edu.ifce.eventif.exception.RegraNegocioException;
import br.edu.ifce.eventif.repository.PerfilRepository;
import br.edu.ifce.eventif.repository.UsuarioRepository;
import br.edu.ifce.eventif.security.PerfilConstants;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de cadastro público (RF01/RN01) e gestão administrativa de perfis (RF04).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        PerfilRepository perfilRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrarParticipante(CadastroUsuarioForm form) {
        String nomeNormalizado = form.getNome().trim();
        String emailNormalizado = form.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new RegraNegocioException("Já existe uma conta cadastrada com este e-mail.");
        }

        Perfil perfilParticipante = perfilRepository.findByNome(PerfilConstants.ROLE_PARTICIPANTE)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_PARTICIPANTE)));

        Usuario usuario = new Usuario(
            nomeNormalizado,
            emailNormalizado,
            passwordEncoder.encode(form.getSenha()),
            true
        );
        usuario.adicionarPerfil(perfilParticipante);

        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new RegraNegocioException("Já existe uma conta cadastrada com este e-mail.");
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioResumoView> listarTodosParaAdmin() {
        return usuarioRepository.findAllByOrderByNomeAsc().stream()
            .map(this::toResumoView)
            .toList();
    }

    @Transactional
    public void concederPerfilOrganizador(Long usuarioAlvoId) {
        Usuario usuario = usuarioRepository.findByIdComPerfis(usuarioAlvoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        validarNaoEhAdmin(usuario);

        Perfil perfilOrganizador = perfilRepository.findByNome(PerfilConstants.ROLE_ORGANIZADOR)
            .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_ORGANIZADOR)));

        usuario.adicionarPerfil(perfilOrganizador);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void removerPerfilOrganizador(Long usuarioAlvoId) {
        Usuario usuario = usuarioRepository.findByIdComPerfis(usuarioAlvoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        validarNaoEhAdmin(usuario);

        usuario.getPerfis().removeIf(perfil -> PerfilConstants.ROLE_ORGANIZADOR.equals(perfil.getNome()));
        if (usuario.getPerfis().isEmpty()) {
            Perfil perfilParticipante = perfilRepository.findByNome(PerfilConstants.ROLE_PARTICIPANTE)
                .orElseGet(() -> perfilRepository.save(new Perfil(PerfilConstants.ROLE_PARTICIPANTE)));
            usuario.adicionarPerfil(perfilParticipante);
        }
        usuarioRepository.save(usuario);
    }

    private void validarNaoEhAdmin(Usuario usuario) {
        boolean ehAdmin = usuario.getPerfis().stream()
            .anyMatch(perfil -> PerfilConstants.ROLE_ADMIN.equals(perfil.getNome()));
        if (ehAdmin) {
            throw new RegraNegocioException(
                "Contas administrativas não podem ter seus perfis alterados por esta operação."
            );
        }
    }

    private UsuarioResumoView toResumoView(Usuario usuario) {
        Set<String> nomesPerfis = usuario.getPerfis().stream()
            .map(Perfil::getNome)
            .collect(Collectors.toSet());

        return new UsuarioResumoView(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.isAtivo(),
            nomesPerfis.contains(PerfilConstants.ROLE_ADMIN),
            nomesPerfis.contains(PerfilConstants.ROLE_ORGANIZADOR),
            nomesPerfis.contains(PerfilConstants.ROLE_PARTICIPANTE)
        );
    }
}
