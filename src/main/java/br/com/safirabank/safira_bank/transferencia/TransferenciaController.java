package br.com.safirabank.safira_bank.transferencia;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transferencias")
public class TransferenciaController {
    private final TransferenciaService service;

    public TransferenciaController(TransferenciaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransferenciaResponse> transferir(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String chave,
            @RequestBody TransferenciaRequest request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(service.transferir(jwt == null ? null : jwt.getSubject(), chave, request));
    }
}