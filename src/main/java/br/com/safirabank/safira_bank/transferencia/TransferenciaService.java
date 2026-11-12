package br.com.safirabank.safira_bank.transferencia;

import br.com.safirabank.safira_bank.conta.*;
import br.com.safirabank.safira_bank.usuario.*;
import br.com.safirabank.safira_bank.transacao.*;
import br.com.safirabank.safira_bank.lancamento.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TransferenciaService {
    private final ContaBancariaRepository contas;
    private final UsuarioRepository usuarios;
    private final TransacaoRepository transacoes;
    private final LancamentoFinanceiroRepository lancamentos;
    private final JdbcTemplate jdbc;
    private final jakarta.persistence.EntityManager entityManager;

    public TransferenciaService(ContaBancariaRepository contas, UsuarioRepository usuarios,
                                TransacaoRepository transacoes, LancamentoFinanceiroRepository lancamentos,
                                JdbcTemplate jdbc, jakarta.persistence.EntityManager entityManager) {
        this.contas = contas;
        this.usuarios = usuarios;
        this.transacoes = transacoes;
        this.lancamentos = lancamentos;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public TransferenciaResponse transferir(String email, String chave, TransferenciaRequest r) {
        if (email == null || email.isBlank()) throw erro(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        Usuario usuario = usuarios.findByEmail(email).orElseThrow(
                () -> erro(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
        if (!"ATIVO".equals(usuario.getStatus())) throw erro(HttpStatus.FORBIDDEN, "Usuário sem acesso ativo.");
        if (r == null || r.contaOrigemId() == null || r.contaOrigemId() <= 0)
            throw erro(HttpStatus.BAD_REQUEST, "Selecione a conta de origem.");
        String agencia = validarTexto(r.agenciaDestino(), 4, "agência de destino");
        String numero = validarTexto(r.numeroDestino(), 12, "conta de destino");
        String descricao = r.descricao() == null ? "" : r.descricao().strip();
        if (descricao.length() > 255) throw erro(HttpStatus.BAD_REQUEST, "A descrição deve ter até 255 caracteres.");
        BigDecimal valor = validarValor(r.valor());
        if (chave == null || !chave.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))
            throw erro(HttpStatus.BAD_REQUEST, "Informe uma chave Idempotency-Key UUID válida.");
        chave = UUID.fromString(chave).toString();
        String hash = hash(r.contaOrigemId() + "\n" + agencia.length() + ":" + agencia + "\n" +
                numero.length() + ":" + numero + "\n" + valor.toPlainString() + "\n" + descricao);

        // ON CONFLICT aguarda a transação concorrente com a mesma chave.
        int inseridas = jdbc.update("""
                INSERT INTO public.transferencia_requisicao(usuario_id,chave,requisicao_hash)
                VALUES (?,?,?) ON CONFLICT (usuario_id,chave) DO NOTHING
                """, usuario.getId(), chave, hash);
        if (inseridas == 0) {
            var anteriores = jdbc.query("""
                    SELECT requisicao_hash,transacao_id FROM public.transferencia_requisicao
                    WHERE usuario_id=? AND chave=?
                    """, (rs, n) -> new Anterior(rs.getString(1), (Long) rs.getObject(2)), usuario.getId(), chave);
            if (anteriores.size() != 1 || !anteriores.get(0).hash().equals(hash))
                throw erro(HttpStatus.CONFLICT, "Chave já utilizada para outra solicitação.");
            Long id = anteriores.get(0).id();
            if (id == null)
                throw erro(HttpStatus.CONFLICT, "Solicitação em processamento. Tente novamente com a mesma chave.");
            return resposta(id);
        }
        ContaBancaria origem = contas.findById(r.contaOrigemId()).filter(
                c -> usuario.getId().equals(c.getUsuario().getId())).orElseThrow(
                () -> erro(HttpStatus.NOT_FOUND, "Conta de origem não encontrada ou indisponível para este usuário."));
        ContaBancaria destino = contas.findByAgenciaAndNumero(agencia, numero).orElseThrow(
                () -> erro(HttpStatus.NOT_FOUND, "Conta de destino não encontrada."));
        if (origem.getId().equals(destino.getId()))
            throw erro(HttpStatus.BAD_REQUEST, "A conta de destino deve ser diferente da origem.");
        Long menor = Math.min(origem.getId(), destino.getId()), maior = Math.max(origem.getId(), destino.getId());
        ContaBancaria primeira = bloquear(menor), segunda = bloquear(maior);
        entityManager.refresh(primeira);
        entityManager.refresh(segunda);
        origem = primeira.getId().equals(r.contaOrigemId()) ? primeira : segunda;
        destino = primeira.getId().equals(destino.getId()) ? primeira : segunda;
        // Revalida após adquirir os bloqueios.
        if (!usuario.getId().equals(origem.getUsuario().getId()))
            throw erro(HttpStatus.FORBIDDEN, "Conta de origem indisponível.");
        if (!"ATIVO".equals(origem.getStatus()) || !"ATIVO".equals(destino.getStatus()))
            throw erro(HttpStatus.BAD_REQUEST, "As contas de origem e destino precisam estar ativas.");
        if (!"ATIVO".equals(destino.getUsuario().getStatus()))
            throw erro(HttpStatus.BAD_REQUEST, "Conta de destino indisponível.");
        BigDecimal saldo = lancamentos.calcularSaldo(origem.getId());
        if (saldo == null || saldo.compareTo(valor) < 0)
            throw erro(HttpStatus.BAD_REQUEST, "Saldo insuficiente para realizar a transferência.");
        LocalDateTime momento = LocalDateTime.now();
        Transacao t = transacoes.saveAndFlush(new Transacao(origem, destino, valor,
                descricao.isBlank() ? "Transferência entre contas" : descricao, momento));
        String detalhe = descricao.isBlank() ? "" : " — " + descricao;
        // Os prefixos também contam no limite de 255 caracteres do banco.
        lancamentos.save(new LancamentoFinanceiro(origem, t, "DEBITO", valor,
                limitar("Transferência enviada" + detalhe), momento));
        lancamentos.save(new LancamentoFinanceiro(destino, t, "CREDITO", valor,
                limitar("Transferência recebida" + detalhe), momento));
        lancamentos.flush();
        jdbc.update("""
                UPDATE public.transferencia_requisicao SET transacao_id=?
                WHERE usuario_id=? AND chave=?
                """, t.getId(), usuario.getId(), chave);
        return resposta(t.getId());
    }

    private ContaBancaria bloquear(Long id) {
        return contas.buscarPorIdComBloqueio(id).orElseThrow(
                () -> erro(HttpStatus.NOT_FOUND, "Uma das contas não foi encontrada."));
    }

    private String validarTexto(String valor, int max, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > max)
            throw erro(HttpStatus.BAD_REQUEST, "Informe uma " + campo + " válida.");
        return valor.strip();
    }

    private BigDecimal validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0)
            throw erro(HttpStatus.BAD_REQUEST, "O valor deve ser maior que zero.");
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um valor exato em centavos.");
        }
        if (valor.precision() > 19) throw erro(HttpStatus.BAD_REQUEST, "Valor acima do limite permitido.");
        return valor;
    }

    private String limitar(String valor) {
        return valor.length() > 255 ? valor.substring(0, 255) : valor;
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private TransferenciaResponse resposta(Long id) {
        return new TransferenciaResponse(id, "CONCLUIDA", "Transferência realizada com sucesso no ambiente demonstrativo.");
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }

    private record Anterior(String hash, Long id) {
    }
}