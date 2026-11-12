package br.com.safirabank.safira_bank.transferencia;

import java.math.BigDecimal;

public record TransferenciaRequest(Long contaOrigemId, String agenciaDestino,
                                   String numeroDestino, BigDecimal valor, String descricao) {
}