package br.com.safirabank.safira_bank.conta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contas")
public class ContaCadastroController {

    private final ContaConsultaService service;

    public ContaCadastroController(ContaConsultaService service) {
        this.service = service;
    }

    @GetMapping("/cadastro-demonstracao")
    public ContaCadastroResponse consultarCadastroDemonstracao() {
        return service.consultarCadastroDemonstracao();
    }
}