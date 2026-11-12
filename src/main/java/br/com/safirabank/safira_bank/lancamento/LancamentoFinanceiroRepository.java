package br.com.safirabank.safira_bank.lancamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LancamentoFinanceiroRepository
        extends JpaRepository<LancamentoFinanceiro, Long> {

    @Query(
            value = """
                    SELECT COALESCE(
                        SUM(
                            CASE
                                WHEN l.tipo = 'CREDITO' THEN l.valor
                                WHEN l.tipo = 'DEBITO' THEN -l.valor
                                ELSE 0
                            END
                        ),
                        0
                    )
                    FROM public.lancamento_financeiro l
                    JOIN public.transacao t
                        ON t.id = l.transacao_id
                    WHERE l.conta_id = :contaId
                      AND t.status = 'CONCLUIDA'
                    """,
            nativeQuery = true
    )
    BigDecimal calcularSaldo(
            @Param("contaId") Long contaId
    );

    List<LancamentoFinanceiro> findAllByOrderByIdDesc();

    @Query("""
            SELECT new br.com.safirabank.safira_bank.lancamento.LancamentoResumoResponse(
                l.id,
                l.criadoEm,
                l.descricao,
                l.tipo,
                l.valor,
                t.status
            )
            FROM LancamentoFinanceiro l
            JOIN l.transacao t
            WHERE l.conta.id = :contaId
            ORDER BY l.criadoEm DESC, l.id DESC
            """)
    List<LancamentoResumoResponse> listarResumoPorConta(
            @Param("contaId") Long contaId
    );
}