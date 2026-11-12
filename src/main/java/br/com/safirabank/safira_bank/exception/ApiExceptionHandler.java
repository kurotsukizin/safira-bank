package br.com.safirabank.safira_bank.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(ResponseStatusException e, HttpServletRequest r) {
        String m = e.getReason();
        if (m == null || m.isBlank()) m = "Não foi possível concluir a solicitação.";
        return ResponseEntity.status(e.getStatusCode()).headers(e.getHeaders()).body(new ErroResponse(Instant.now(), e.getStatusCode().value(), m, r.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException e, HttpServletRequest r) {
        return ResponseEntity.badRequest().body(new ErroResponse(Instant.now(), 400, "Corpo da requisição ausente ou inválido. Envie um JSON válido.", r.getRequestURI()));
    }

    public record ErroResponse(Instant timestamp, int status, String mensagem, String path) {
    }
}
