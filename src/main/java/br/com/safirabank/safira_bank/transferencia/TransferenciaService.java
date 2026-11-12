package br.com.safirabank.safira_bank.transferencia;

import br.com.safirabank.safira_bank.conta.ContaBancaria;
import br.com.safirabank.safira_bank.conta.ContaBancariaRepository;
import br.com.safirabank.safira_bank.lancamento.LancamentoFinanceiro;
import br.com.safirabank.safira_bank.lancamento.LancamentoFinanceiroRepository;
import br.com.safirabank.safira_bank.transacao.Transacao;
import br.com.safirabank.safira_bank.transacao.TransacaoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class TransferenciaService {

    private static final String AGENCIA_DEMONSTRACAO = "0001";
    private static final String NUMERO_DEMONSTRACAO = "123456";

    private final ContaBancariaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;
    private final LancamentoFinanceiroRepository lancamentoRepository;

    public TransferenciaService(
            ContaBancariaRepository contaRepository,
            TransacaoRepository transacaoRepository,
            LancamentoFinanceiroRepository lancamentoRepository
    ) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        this.lancamentoRepository = lancamentoRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long transferirDemonstracao(
            String agenciaDestino,
            String numeroDestino,
            BigDecimal valor
    ) {
        BigDecimal valorValidado = validarValor(valor);

        if (agenciaDestino == null || agenciaDestino.isBlank()) {
            throw erroValidacao("Informe a agência de destino.");
        }

        if (numeroDestino == null || numeroDestino.isBlank()) {
            throw erroValidacao("Informe o número da conta de destino.");
        }

        ContaBancaria origem = contaRepository
                .findByAgenciaAndNumero(
                        AGENCIA_DEMONSTRACAO,
                        NUMERO_DEMONSTRACAO
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conta demonstrativa não cadastrada."
                ));

        ContaBancaria destino = contaRepository
                .findByAgenciaAndNumero(
                        agenciaDestino.trim(),
                        numeroDestino.trim()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conta de destino não encontrada."
                ));

        if (origem.getId().equals(destino.getId())) {
            throw erroValidacao(
                    "A conta de destino deve ser diferente da origem."
            );
        }

        // Adquire os bloqueios sempre na mesma ordem.
        Long primeiroId = Math.min(origem.getId(), destino.getId());
        Long segundoId = Math.max(origem.getId(), destino.getId());

        ContaBancaria primeira = bloquearConta(primeiroId);
        ContaBancaria segunda = bloquearConta(segundoId);

        ContaBancaria origemBloqueada =
                primeira.getId().equals(origem.getId())
                        ? primeira
                        : segunda;

        ContaBancaria destinoBloqueada =
                primeira.getId().equals(destino.getId())
                        ? primeira
                        : segunda;

        BigDecimal saldo = lancamentoRepository
                .calcularSaldo(origemBloqueada.getId());

        if (saldo.compareTo(valorValidado) < 0) {
            throw erroValidacao(
                    "Saldo insuficiente para realizar a transferência."
            );
        }

        LocalDateTime momento = LocalDateTime.now();

        Transacao transacao = new Transacao(
                origemBloqueada,
                destinoBloqueada,
                valorValidado,
                "Transferência da conta demonstrativa",
                momento
        );

        transacao = transacaoRepository.save(transacao);

        LancamentoFinanceiro debito = new LancamentoFinanceiro(
                origemBloqueada,
                transacao,
                "DEBITO",
                valorValidado,
                "Transferência enviada",
                momento
        );

        LancamentoFinanceiro credito = new LancamentoFinanceiro(
                destinoBloqueada,
                transacao,
                "CREDITO",
                valorValidado,
                "Transferência recebida",
                momento
        );

        lancamentoRepository.save(debito);
        lancamentoRepository.save(credito);

        return transacao.getId();
    }

    private ContaBancaria bloquearConta(Long id) {
        return contaRepository
                .buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Uma das contas não foi encontrada."
                ));
    }

    private BigDecimal validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw erroValidacao(
                    "O valor da transferência deve ser maior que zero."
            );
        }

        BigDecimal valorValidado;

        try {
            valorValidado = valor.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException excecao) {
            throw erroValidacao(
                    "O valor deve representar uma quantia exata em centavos."
            );
        }

        if (valorValidado.precision() > 19) {
            throw erroValidacao(
                    "O valor ultrapassa o limite permitido."
            );
        }

        return valorValidado;
    }

    private ResponseStatusException erroValidacao(String mensagem) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensagem
        );
    }
}