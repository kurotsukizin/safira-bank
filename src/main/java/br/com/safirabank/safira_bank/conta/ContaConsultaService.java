package br.com.safirabank.safira_bank.conta;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContaConsultaService {

    private final ContaBancariaRepository repository;

    public ContaConsultaService(ContaBancariaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ContaCadastroResponse consultarCadastroDemonstracao() {
        ContaBancaria conta = repository
                .findByAgenciaAndNumero("0001", "123456")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conta demonstrativa não cadastrada"
                ));

        return new ContaCadastroResponse(
                conta.getId(),
                conta.getUsuario().getNome(),
                conta.getAgencia(),
                conta.getNumero(),
                conta.getStatus()
        );
    }
}