package br.com.safirabank.safira_bank.status;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public Map<String, String> getStatus() {
        return Map.of(
                "application", "Safira Bank",
                "status", "UP",
                "message", "API do banco simulado em funcionamento"
        );
    }
}