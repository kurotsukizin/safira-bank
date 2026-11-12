package br.com.safirabank.safira_bank.auth;

import br.com.safirabank.safira_bank.config.JwtConfig;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long VALIDADE_SEGUNDOS = 900;

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        if (request == null
                || request.email() == null
                || request.email().isBlank()
                || request.senha() == null
                || request.senha().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe e-mail e senha."
            );
        }

        Authentication autenticacao;

        try {
            autenticacao = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken
                            .unauthenticated(
                                    request.email().strip(),
                                    request.senha()
                            )
            );
        } catch (AuthenticationException erro) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciais inválidas."
            );
        }

        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISSOR)
                .subject(autenticacao.getName())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(VALIDADE_SEGUNDOS))
                .build();

        JwsHeader cabecalho = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(cabecalho, claims)
        ).getTokenValue();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new LoginResponse(
                        token,
                        "Bearer",
                        VALIDADE_SEGUNDOS
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "email", jwt.getSubject(),
                        "mensagem", "Token validado com sucesso."
                ));
    }

    public record LoginRequest(
            String email,
            String senha
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn
    ) {
    }
}