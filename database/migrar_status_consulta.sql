-- MIGRAÇÃO PARA BANCO CRIADO EM VERSÕES ANTIGAS DO PROJETO.
-- Execute SOMENTE se diagnostico_status_consulta.sql revelar
-- uma restrição CHECK de consultas.status sem os novos valores.
-- Faça backup antes. Não apaga consultas, pacientes nem pagamentos.

BEGIN;
DO $$
DECLARE item record;
BEGIN
  FOR item IN
    SELECT conname, pg_get_constraintdef(oid) AS definicao
    FROM pg_constraint
    WHERE conrelid = 'consultas'::regclass
      AND contype = 'c'
  LOOP
    -- Remove apenas restrições CHECK de enumeracao do status que
    -- já contenham ao menos dois status conhecidos.
    IF item.definicao ILIKE '%status%'
       AND item.definicao LIKE '%AGENDADA%'
       AND item.definicao LIKE '%CANCELADA%'
       AND (item.definicao NOT LIKE '%AGUARDANDO%'
            OR item.definicao NOT LIKE '%EM_ATENDIMENTO%') THEN
      EXECUTE format('ALTER TABLE consultas DROP CONSTRAINT %I', item.conname);
    END IF;
  END LOOP;
END $$;

-- Evita criar uma segunda CHECK equivalente se o banco já estiver correto.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'consultas'::regclass AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%AGUARDANDO%'
      AND pg_get_constraintdef(oid) LIKE '%EM_ATENDIMENTO%'
      AND pg_get_constraintdef(oid) LIKE '%status%'
  ) THEN
    ALTER TABLE consultas ADD CONSTRAINT ck_consultas_status_atendimento
      CHECK (status IN ('AGENDADA', 'CONFIRMADA', 'AGUARDANDO',
                       'EM_ATENDIMENTO', 'REALIZADA', 'CANCELADA'));
  END IF;
END $$;
COMMIT;
