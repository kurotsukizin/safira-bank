package br.com.safirabank.safira_bank;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarHashSenha {
    public static void main(String[] args) {
        var console = System.console();
        if (console == null) throw new IllegalStateException("Execute em terminal interativo com console disponível.");
        char[] entrada = console.readPassword("Senha fictícia para gerar o hash: ");
        if (entrada == null || entrada.length == 0) throw new IllegalArgumentException("Informe uma senha.");
        try {
            System.out.println(new BCryptPasswordEncoder().encode(new String(entrada)));
        } finally {
            java.util.Arrays.fill(entrada, '\0');
        }
    }
}