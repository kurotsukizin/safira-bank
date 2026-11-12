package br.com.safirabank.safira_bank.transacao;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoRepository
        extends JpaRepository<Transacao, Long> {
}