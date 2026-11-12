package br.com.safirabank.safira_bank.lancamento;

import br.com.safirabank.safira_bank.conta.ContaBancaria;
import br.com.safirabank.safira_bank.conta.ContaBancariaRepository;
import br.com.safirabank.safira_bank.usuario.Usuario;
import br.com.safirabank.safira_bank.usuario.UsuarioRepository;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoFinanceiroController {

    private final ContaBancariaRepository contaRepository;
    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final UsuarioRepository usuarioRepository;

    public LancamentoFinanceiroController(
            ContaBancariaRepository contaRepository,
            LancamentoFinanceiroRepository lancamentoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.contaRepository = contaRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LancamentoResumoResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("contaId") Long contaId
    ) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Autenticação necessária."
            );
        }

        if (contaId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe uma conta válida."
            );
        }

        Usuario usuario = usuarioRepository
                .findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não encontrado."
                ));

        if (!"ATIVO".equals(usuario.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuário sem acesso ativo."
            );
        }

        ContaBancaria conta = contaRepository
                .findById(contaId)
                .filter(contaEncontrada ->
                        usuario.getId().equals(
                                contaEncontrada.getUsuario().getId()
                        )
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conta não encontrada ou indisponível para este usuário."
                ));

        return lancamentoRepository.listarResumoPorConta(
                conta.getId()
        );
    }
}