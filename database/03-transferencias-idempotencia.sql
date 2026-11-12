-- NOVO: executar manualmente uma vez no banco usado pela aplicação.
-- Não recria tabelas de usuário/conta e não altera saldos.
BEGIN;
CREATE TABLE IF NOT EXISTS public.transferencia_requisicao (
 usuario_id BIGINT NOT NULL REFERENCES public.usuario(id),
 chave VARCHAR(36) NOT NULL,
 requisicao_hash VARCHAR(64) NOT NULL,
 transacao_id BIGINT REFERENCES public.transacao(id),
 criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(usuario_id,chave)
);
COMMIT;
