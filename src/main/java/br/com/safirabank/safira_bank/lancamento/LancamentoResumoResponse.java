package br.com.safirabank.safira_bank.lancamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LancamentoResumoResponse(Long id, LocalDateTime criadoEm, String descricao, String tipo, BigDecimal valor,
                                       String status) {
}