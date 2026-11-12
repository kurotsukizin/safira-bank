
BEGIN;
INSERT INTO public.usuario(nome,email,senha_hash,perfil,status,criado_em)
VALUES ('Ana Demonstração','ana.demo@exemplo.com',:'senha_hash','CLIENTE','ATIVO',CURRENT_TIMESTAMP),
       ('Bruno Demonstração','bruno.demo@exemplo.com',:'senha_hash','CLIENTE','ATIVO',CURRENT_TIMESTAMP);
INSERT INTO public.conta_bancaria(agencia,numero,status,criada_em,usuario_id)
SELECT '0001','123456','ATIVO',CURRENT_TIMESTAMP,id FROM public.usuario WHERE email='ana.demo@exemplo.com';
INSERT INTO public.conta_bancaria(agencia,numero,status,criada_em,usuario_id)
SELECT '0001','654321','ATIVO',CURRENT_TIMESTAMP,id FROM public.usuario WHERE email='bruno.demo@exemplo.com';
COMMIT;
