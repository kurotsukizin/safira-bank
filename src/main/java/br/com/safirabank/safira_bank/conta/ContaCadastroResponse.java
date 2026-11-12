package br.com.safirabank.safira_bank.conta;

public record ContaCadastroResponse(
        Long id,
        String titular,
        String agencia,
        String numero,
        String status
) {
}