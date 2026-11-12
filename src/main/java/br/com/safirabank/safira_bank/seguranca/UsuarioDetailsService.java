package br.com.safirabank.safira_bank.seguranca;

import br.com.safirabank.safira_bank.usuario.Usuario;
import br.com.safirabank.safira_bank.usuario.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException(
                    "Credenciais inválidas."
            );
        }

        Usuario usuario = usuarioRepository
                .findByEmail(email.strip())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Credenciais inválidas."
                ));

        boolean usuarioAtivo =
                "ATIVO".equals(usuario.getStatus());

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .authorities("CONTA_ACESSO")
                .disabled(!usuarioAtivo)
                .build();
    }
}