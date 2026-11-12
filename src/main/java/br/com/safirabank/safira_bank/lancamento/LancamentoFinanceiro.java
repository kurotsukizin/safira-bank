package br.com.safirabank.safira_bank.lancamento;

import br.com.safirabank.safira_bank.conta.ContaBancaria;
import br.com.safirabank.safira_bank.transacao.Transacao;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamento_financeiro", schema = "public")
public class LancamentoFinanceiro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String tipo;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    @Column(length = 255)
    private String descricao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private ContaBancaria conta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transacao_id", nullable = false)
    private Transacao transacao;

    protected LancamentoFinanceiro() {
    }

    public LancamentoFinanceiro(ContaBancaria conta, Transacao transacao, String tipo, BigDecimal valor, String descricao, LocalDateTime momento) {
        if (!"CREDITO".equals(tipo) && !"DEBITO".equals(tipo))
            throw new IllegalArgumentException("O tipo do lançamento deve ser CREDITO ou DEBITO.");
        if (valor == null || valor.signum() <= 0)
            throw new IllegalArgumentException("O valor do lançamento deve ser positivo.");
        this.conta = conta;
        this.transacao = transacao;
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
        this.criadoEm = momento;
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getDescricao() {
        return descricao;
    }

    public ContaBancaria getConta() {
        return conta;
    }

    public Transacao getTransacao() {
        return transacao;
    }
}