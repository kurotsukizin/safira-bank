package br.com.safirabank.safira_bank.conta;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContaBancariaRepository
        extends JpaRepository<ContaBancaria, Long> {

    Optional<ContaBancaria> findByAgenciaAndNumero(
            String agencia,
            String numero
    );

    @Query("""
            SELECT c
            FROM ContaBancaria c
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.id ASC
            """)
    List<ContaBancaria> listarPorUsuarioId(
            @Param("usuarioId") Long usuarioId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM ContaBancaria c
            WHERE c.id = :id
            """)
    Optional<ContaBancaria> buscarPorIdComBloqueio(
            @Param("id") Long id
    );
}