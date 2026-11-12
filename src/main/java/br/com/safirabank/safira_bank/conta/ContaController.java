package br.com.safirabank.safira_bank.conta;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final ContaSaldoService service;

    public ContaController(ContaSaldoService service) {
        this.service = service;
    }

    @GetMapping("/demonstracao")
    public ContaResumoResponse consultarContaDemonstracao() {
        return service.consultarResumoDemonstracao();
    }

    @GetMapping("/minhas")
    public List<ContaResumoResponse> consultarMinhasContas(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.consultarMinhasContas(
                jwt.getSubject()
        );
    }
}