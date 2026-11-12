package br.com.safirabank.safira_bank.transferencia;

public record TransferenciaResponse(
        Long transacaoId,
        String status,
        String mensagem
) {
}