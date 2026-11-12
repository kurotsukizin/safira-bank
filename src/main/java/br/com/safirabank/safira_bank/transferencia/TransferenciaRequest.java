package br.com.safirabank.safira_bank.transferencia;

import java.math.BigDecimal;

public record TransferenciaRequest(
        String agenciaDestino,
        String numeroDestino,
        BigDecimal valor
) {
}