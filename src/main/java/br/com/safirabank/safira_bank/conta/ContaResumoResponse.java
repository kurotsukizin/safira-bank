package br.com.safirabank.safira_bank.conta;

import java.math.BigDecimal;

public record ContaResumoResponse(
        Long id,
        String titular,
        String agencia,
        String numero,
        BigDecimal saldo,
        boolean demonstracao
) {
}