package br.com.safirabank.safira_bank.usuario;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario", schema = "public")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;
    @Column(nullable = false, length = 20)
    private String perfil;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Usuario() {
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getPerfil() {
        return perfil;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    @JsonIgnore
    public String getSenhaHash() {
        return senhaHash;
    }
}