# Roteiro de apresentação — OdontoCare

O objetivo é apresentar os quatro perfis e o relacionamento entre os módulos **pelo navegador**, sem depender do Postman.

## 0. Preparação

Execute `mvn clean test`, inicie a aplicação e confirme `http://localhost:8080`. Use o banco de testes existente. Cadastre (se necessário) pelo administrador: uma recepcionista, um paciente com conta vinculada, um dentista, um procedimento de 60 minutos e uma escala ativa para o dia. Não compartilhe senhas/tokens durante a apresentação.

## 1. Administrador

1. Acesse o painel. Confira o menu por perfil e os números reais de pacientes, dentistas e cobranças pendentes.
2. Abra **Procedimentos** e mostre duração e valor.
3. Abra **Escalas mensais**. Gere dias alternados para dois dentistas, com pausas em horários diferentes (12h–13h e 13h–14h), e altere uma data para folga/afastamento.
4. Abra **Relatórios**. Filtre o mês. Confira faturamento confirmado, ticket médio, procedimentos realizados e atendimentos por dentista. Exporte CSV.
5. Confirme que a agenda operacional não aparece no menu do administrador.

## 2. Recepcionista

1. Entre com uma conta `RECEPCIONISTA`.
2. Abra **Agenda**, escolha a data e verifique que só aparecem profissionais escalados.
3. Selecione dentista, procedimento e um horário disponível. Cadastre uma consulta para paciente de teste. Confirme que ela aparece apenas uma vez em **Consultas do dia**.
4. Abra novamente a mesma data e tente um agendamento que sobreponha o anterior: deve ser impedido.
5. Abra **Cobranças**. Verifique a cobrança automática com o valor congelado no agendamento.
6. Abra **Andamento das consultas** no dia do atendimento e clique em **Registrar chegada**. A consulta passa a `AGUARDANDO` e o progresso avança.
7. Abra **Chat com pacientes**, selecione o paciente e envie uma mensagem.

## 3. Dentista

1. Entre como o dentista escalado. Na **Minha agenda**, localize a consulta.
2. Abra **Histórico** para consultar prontuários anteriores daquele paciente.
3. Abra **Atender**, inicie o atendimento e salve diagnóstico/tratamento/observações no prontuário.
4. Confirme que a consulta passa para `REALIZADA`.
5. Abra **Meus atendimentos** e filtre por data inicial/final. Confira a consulta concluída e o link para o prontuário.

## 4. Paciente

1. Entre em uma janela anônima com a conta do paciente.
2. Abra **Minhas consultas**: exibição limitada às consultas do próprio paciente.
3. Abra **Meus pagamentos**, selecione a cobrança e verifique valor/status.
4. Caso tenha `MP_ACCESS_TOKEN` de teste, clique em **Prosseguir para pagamento seguro**. O navegador deve abrir o checkout do Mercado Pago. Use apenas conta de comprador e meios de pagamento de teste; para consultar o resultado, volte à cobrança e clique **Verificar status** nas tentativas pendentes. **Não aceite o retorno do navegador sozinho como confirmação**.
5. Abra **Mensagens** e responda à recepção. Na outra janela, a mensagem deve surgir em poucos segundos.
6. Confira **Meus dados**.

## 5. Casos de validação negativa

- Paciente tentando abrir `/admin/relatorios` → acesso negado.
- Dentista tentando abrir `/pagamentos` → acesso negado.
- Recepção tentando acessar `/historico-pacientes` → acesso negado.
- Agendamento durante almoço, fora da jornada, com dentista não escalado ou sobreposto a outra consulta → rejeitado.
- Registro de chegada em data diferente da consulta → rejeitado.
- Cobrança já paga ou com tentativa online → bloqueio de registro presencial indevido.
- Paciente não pode abrir cobranças ou conversas de outro paciente.

## 6. Diagnóstico de erros

- `BUILD FAILURE`: copie o primeiro erro Java real do Maven.
- `database ... does not exist`: crie `clinica_odontologica` no pgAdmin.
- `consultas_status_check`: faça backup e execute a migração em `database/migrar_status_consulta.sql`.
- `Pagamento online indisponível`: configure a variável de ambiente `MP_ACCESS_TOKEN` e reinicie o Spring Boot no mesmo terminal.
- Checkout não abre: confira se o Access Token é de teste e se a API do Mercado Pago devolveu erro no terminal. Não compartilhe credenciais.
