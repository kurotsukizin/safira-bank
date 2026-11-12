package br.com.safirabank.safira_bank.transferencia;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/transferencias")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    public TransferenciaController(
            TransferenciaService transferenciaService
    ) {
        this.transferenciaService = transferenciaService;
    }

    @PostMapping
    public ResponseEntity<TransferenciaResponse> transferir(
            @RequestBody TransferenciaRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe os dados da transferência."
            );
        }

        Long transacaoId = transferenciaService
                .transferirDemonstracao(
                        request.agenciaDestino(),
                        request.numeroDestino(),
                        request.valor()
                );

        TransferenciaResponse response = new TransferenciaResponse(
                transacaoId,
                "CONCLUIDA",
                "Transferência de demonstração realizada com sucesso."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}