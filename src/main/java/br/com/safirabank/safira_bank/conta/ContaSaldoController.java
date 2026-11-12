package br.com.safirabank.safira_bank.conta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contas")
public class ContaSaldoController {

    private final ContaSaldoService service;

    public ContaSaldoController(ContaSaldoService service) {
        this.service = service;
    }

    @GetMapping("/resumo-demonstracao")
    public ContaResumoResponse consultarResumoDemonstracao() {
        return service.consultarResumoDemonstracao();
    }
}