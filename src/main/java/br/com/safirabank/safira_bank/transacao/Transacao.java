package br.com.safirabank.safira_bank.transacao;

import br.com.safirabank.safira_bank.conta.ContaBancaria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacao", schema = "public")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_origem_id", nullable = false)
    private ContaBancaria contaOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_destino_id", nullable = false)
    private ContaBancaria contaDestino;

    protected Transacao() {
    }

    public Transacao(
            ContaBancaria contaOrigem,
            ContaBancaria contaDestino,
            BigDecimal valor,
            String descricao,
            LocalDateTime momento
    ) {
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.descricao = descricao;
        this.status = "CONCLUIDA";
        this.criadaEm = momento;
        this.concluidaEm = momento;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getConcluidaEm() {
        return concluidaEm;
    }

    public String getDescricao() {
        return descricao;
    }

    public ContaBancaria getContaOrigem() {
        return contaOrigem;
    }

    public ContaBancaria getContaDestino() {
        return contaDestino;
    }
}