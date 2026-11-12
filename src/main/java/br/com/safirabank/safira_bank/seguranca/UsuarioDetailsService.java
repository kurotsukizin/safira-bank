package br.com.safirabank.safira_bank.seguranca;

import br.com.safirabank.safira_bank.usuario.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository r) {
        this.usuarioRepository = r;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        if (email == null || email.isBlank()) throw new UsernameNotFoundException("Credenciais inválidas.");
        Usuario u = usuarioRepository.findByEmail(email.strip()).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return User.withUsername(u.getEmail()).password(u.getSenhaHash()).authorities("CONTA_ACESSO").disabled(!"ATIVO".equals(u.getStatus())).build();
    }
}