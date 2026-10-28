# Cobranças automáticas — versão 3.2

## Regra de negócio

- No agendamento de consulta com procedimento de valor positivo, o sistema cria `pagamentos` automaticamente com status `PENDENTE`. O valor fica congelado no momento do agendamento, mesmo se o preço do procedimento mudar.
- Ao abrir a aplicação, são criadas cobranças pendentes de consultas antigas que ainda não as possuíam. Cobranças existentes são mantidas.
- Cancelar uma consulta pendente sem tentativas online cancela também sua cobrança. Consultas pagas ou com tentativas online exigem regularização financeira antes do cancelamento.
- Recepção: tela **Pagamentos** é listagem de cobranças. Pode confirmar **recebimento presencial** com forma de pagamento escolhida; não pode inventar valores, apagar cobrança ou marcar online como pago.
- Paciente: **Meus pagamentos** permite pagar com checkout do Mercado Pago; confirmação do pagamento é obtida pela API do Mercado Pago, por webhook verificado ou por consulta manual autenticada ao provedor.

## Por que aparecia "Pagamento online indisponível"?

`MP_ACCESS_TOKEN` estava em branco. Para usar a API, crie uma aplicação no Mercado Pago Developers e use somente **credenciais de teste** durante o desenvolvimento. **Nunca envie seu Access Token por chat, e-mail, print ou commit Git.**

No PowerShell aberto na pasta do projeto (token de exemplo abaixo NÃO é válido):

```powershell
$env:MP_ACCESS_TOKEN = 'SEU_ACCESS_TOKEN_DE_TESTE'
mvn spring-boot:run
```

A sessão do terminal precisa permanecer aberta; variáveis definidas no terminal não chegam automaticamente ao botão Run Java de outra sessão.

* `MP_PUBLIC_URL` pode ficar vazio para teste inicial local. Assim, não haverá webhook externo; depois de concluir o checkout, volte a **Meus pagamentos > Ver cobrança > Atualizar status no Mercado Pago**.
* Para webhook automático, exponha a aplicação com HTTPS público de forma temporária e configure `MP_PUBLIC_URL` e `MP_WEBHOOK_SECRET`, com notificação apontando para `/api/pagamentos/webhook`. Não exponha o PostgreSQL nem publique credenciais.
* O Mercado Pago recomenda contas de teste de **vendedor** e **comprador**, distintas. Confira as instruções oficiais atualizadas em https://www.mercadopago.com.br/developers/pt/docs/checkout-pro-preferences/test-accounts e https://www.mercadopago.com.br/developers/pt/docs/checkout-pro-preferences/integration-test/introduction
* A integração usa Checkout Pro pela API de preferências já existente no projeto. Na documentação oficial, esse caminho está marcado como fluxo clássico/legacy e o provedor recomenda Orders API para novos projetos. Mantivemos o fluxo existente para evitar refatoração grande neste trabalho acadêmico.

## Roteiro de testes

1. `mvn clean test` → `BUILD SUCCESS`.
2. `mvn spring-boot:run` e login recepcionista.
3. Agende um procedimento de R$ 150,00 para o paciente João; **não abra Pagamentos antes**.
4. Na aba **Pagamentos**, verifique que existe uma única cobrança pendente de R$ 150,00, vinculada à consulta. Não deve existir botão Novo pagamento.
5. Como paciente João, abra **Meus pagamentos**; cobrança deve aparecer automaticamente, mesmo sem visita à tela financeira da recepção.
6. Configure **somente token de teste** conforme acima e reinicie a aplicação.
7. Como João, clique **Ver cobrança > Pagar online**; o checkout hospedado do Mercado Pago deverá abrir.
8. Use uma conta e um cartão **de teste** do Mercado Pago. Nunca cartão real neste ambiente.
9. Retorne ao sistema como João e use **Atualizar status no Mercado Pago**, enquanto o webhook ainda não estiver configurado. O estado deverá passar a `PAGO` somente se o provedor confirmar `approved` e os valores coincidirem.
10. Na recepção, a mesma cobrança fica `PAGO`; relatórios passam a computar o valor dentro do período de recebimento.
11. Em outra consulta sem tentativa online, confirme pagamento presencial e verifique `PAGO`.
12. Tente cancelar uma consulta pendente sem tentativa online: a cobrança deve ficar `CANCELADO`.

**Observação:** o provedor pode alterar regras e telas de teste. Consulte a documentação oficial antes de inserir credenciais e não use ambientes de produção para simulações.
