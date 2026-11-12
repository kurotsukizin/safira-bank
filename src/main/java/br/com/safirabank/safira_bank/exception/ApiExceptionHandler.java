package br.com.safirabank.safira_bank.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        String mensagem = exception.getReason();

        if (mensagem == null || mensagem.isBlank()) {
            mensagem = "Não foi possível concluir a solicitação.";
        }

        ErroResponse resposta = new ErroResponse(
                Instant.now(),
                exception.getStatusCode().value(),
                mensagem,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .headers(exception.getHeaders())
                .body(resposta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ErroResponse resposta = new ErroResponse(
                Instant.now(),
                400,
                "Corpo da requisição ausente ou inválido. Envie um JSON válido.",
                request.getRequestURI()
        );

        return ResponseEntity
                .badRequest()
                .body(resposta);
    }

    public record ErroResponse(
            Instant timestamp,
            int status,
            String mensagem,
            String path
    ) {
    }
}