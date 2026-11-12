package br.com.safirabank.safira_bank;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarHashSenha {

    public static void main(String[] args) {
        String senhaDeTeste = "Safira-Demo!2026";

        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        String hash = encoder.encode(senhaDeTeste);

        System.out.println("Hash para o usuário fictício:");
        System.out.println(hash);

        System.out.println(
                "Verificação: " +
                        encoder.matches(senhaDeTeste, hash)
        );
    }
}