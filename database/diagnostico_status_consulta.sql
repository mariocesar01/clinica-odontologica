-- Execute no Query Tool do banco clinica_odontologica.
-- Este comando APENAS consulta as restrições; não altera dados.
SELECT conname AS nome_restricao,
       pg_get_constraintdef(oid) AS definicao
FROM pg_constraint
WHERE conrelid = 'consultas'::regclass
  AND contype = 'c'
ORDER BY conname;

-- Se a restrição de status NÃO inclui AGUARDANDO e EM_ATENDIMENTO,
-- execute o script migrar_status_consulta.sql depois de fazer backup.
