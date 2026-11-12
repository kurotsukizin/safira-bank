package br.com.safirabank.safira_bank.conta;

import br.com.safirabank.safira_bank.lancamento.LancamentoFinanceiroRepository;
import br.com.safirabank.safira_bank.usuario.Usuario;
import br.com.safirabank.safira_bank.usuario.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContaSaldoService {

    private final ContaBancariaRepository contaRepository;
    private final LancamentoFinanceiroRepository lancamentoRepository;
    private final UsuarioRepository usuarioRepository;

    public ContaSaldoService(
            ContaBancariaRepository contaRepository,
            LancamentoFinanceiroRepository lancamentoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.contaRepository = contaRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public ContaResumoResponse consultarResumoDemonstracao() {
        ContaBancaria conta = contaRepository
                .findByAgenciaAndNumero("0001", "123456")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conta demonstrativa não cadastrada."
                ));

        return montarResumo(conta);
    }

    @Transactional(readOnly = true)
    public List<ContaResumoResponse> consultarMinhasContas(
            String emailAutenticado
    ) {
        Usuario usuario = usuarioRepository
                .findByEmail(emailAutenticado)
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

        return contaRepository
                .listarPorUsuarioId(usuario.getId())
                .stream()
                .map(this::montarResumo)
                .toList();
    }

    private ContaResumoResponse montarResumo(
            ContaBancaria conta
    ) {
        BigDecimal saldo = lancamentoRepository
                .calcularSaldo(conta.getId())
                .setScale(2);

        return new ContaResumoResponse(
                conta.getId(),
                conta.getUsuario().getNome(),
                conta.getAgencia(),
                conta.getNumero(),
                saldo,
                true
        );
    }
}