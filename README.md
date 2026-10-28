# OdontoCare — Sistema Web de Clínica Odontológica

Versão consolidada para apresentação acadêmica, incluindo **back-end Java/Spring Boot**, **front-end Thymeleaf + CSS + JavaScript** e **PostgreSQL**. Desenvolvida a partir do projeto anterior, sem substituir o modelo de dados ou os fluxos já validados.

## Tecnologias

- Java 21, Spring Boot 3.3.4, Maven, Spring MVC, Spring Data JPA e Hibernate
- PostgreSQL, Thymeleaf, HTML, CSS e JavaScript sem frameworks de interface
- Senhas com BCrypt (`spring-security-crypto`); autenticação simples por `HttpSession` + interceptor
- Integração de pagamentos com Mercado Pago Checkout Pro via REST
- Chat persistido no PostgreSQL, com atualização periódica a cada 5 segundos

Arquitetura preservada: **Controller → Service → Repository → PostgreSQL**. Os templates ficam em `src/main/resources/templates`, o estilo em `static/css/app.css` e os scripts em `static/js`.

## Funcionalidades por perfil

| Perfil | Telas e permissões |
|---|---|
| Administrador | Administradores, recepcionistas, pacientes, dentistas, procedimentos (valor e duração), escalas mensais e relatórios com exportação CSV |
| Recepcionista | Pacientes, agenda por data com profissionais escalados e horários calculados, consultas, cobranças automáticas/recebimento presencial, andamento das consultas e chat |
| Dentista | Minha agenda, iniciar/finalizar atendimento, prontuário ligado à consulta, histórico clínico do paciente e meus atendimentos com filtro por período |
| Paciente | Consultas próprias, cobranças automáticas, checkout online, tentativas de pagamento, chat com a recepção e dados cadastrais |

Todos os perfis possuem **painel inicial, menu lateral responsivo e acesso rápido**. Os dados dos indicadores são consultados no banco, não são valores fictícios.

### Regras da agenda

- A escala é cadastrada pelo administrador **mês a mês**, com geração por dias da semana e ajustes por data.
- Permite horários diferentes, intervalos de almoço, folga e afastamento por dentista.
- A recepção escolhe primeiro a **data**; só aparecem dentistas escalados.
- O procedimento determina a duração; o agendamento bloqueia o intervalo completo, sem sobreposição com consultas, jornada ou pausas.
- As consultas do dia são mostradas **uma única vez**, separadas das opções de início de novo agendamento.

### Fluxo do atendimento

`AGENDADA → CONFIRMADA (opcional) → AGUARDANDO → EM_ATENDIMENTO → REALIZADA`.

A recepção registra a chegada na data da consulta. O dentista inicia a consulta e salva o prontuário para concluí-la. A barra de progresso acompanha as etapas. Consultas canceladas não contam no progresso médio.

### Cobranças e pagamentos

- Uma cobrança **PENDENTE é criada automaticamente** ao agendar uma consulta (inclui regularização sem duplicação de consultas anteriores).
- O valor é uma cópia do preço do procedimento no momento do agendamento.
- A recepção pode registrar pagamento presencial quando **não** existe tentativa online vinculada.
- O paciente acessa a cobrança pelo navegador e pode iniciar o Checkout Pro.
- **Retorno do navegador não comprova pagamento.** A atualização para `PAGO` depende de consulta validada à API do Mercado Pago (ID, referência externa, moeda e valor).
- O sistema registra tentativas; o webhook valida assinatura HMAC, e o paciente pode solicitar reconsulta manual ao provedor quando estiver testando pelo localhost.
- Pagamento online requer **seu próprio Access Token**, e não há cobrança real sem uma transação feita no provedor. Transações reais podem ter tarifas do Mercado Pago.

## Iniciar no Windows / VS Code

1. Abra **a pasta que contém `pom.xml`**.
2. Confirme Java 21, Maven e PostgreSQL funcionando.
3. No pgAdmin, crie o banco `clinica_odontologica`, caso ainda não exista. **Não precisa recriar tabelas**.
4. Confira as configurações de `src/main/resources/application.properties`. A senha local pode ser definida com `DB_PASSWORD` (valor padrão: `postgres`, apenas desenvolvimento).
5. Execute `mvn clean test` e, depois, `mvn spring-boot:run`.
6. Abra `http://localhost:8080`.

Se a tabela `usuarios` estiver vazia, o sistema cria o administrador inicial **admin@clinica.com** com senha **admin123**. **Altere essa senha ao preparar uma apresentação com acesso de terceiros.** Não use essa conta padrão em um sistema publicado na internet.

**Banco antigo:** se aparecer o erro `consultas_status_check` ao registrar chegada, execute no pgAdmin `database/migrar_status_consulta.sql` **depois de fazer backup**. A migração só atua nas restrições antigas do status. Em um banco recém-criado, o Hibernate gera o esquema com os estados atuais. Nunca apague o banco apenas para trocar de versão.

## Mercado Pago: ambiente de testes

O projeto lê credenciais por variáveis de ambiente, não pelo repositório. No **PowerShell** do VS Code:

```powershell
$env:MP_ACCESS_TOKEN = "SEU_ACCESS_TOKEN_DE_TESTE"
mvn spring-boot:run
```

- Crie uma aplicação para Checkout Pro no [Mercado Pago Developers](https://www.mercadopago.com.br/developers/pt).
- Use **credenciais de teste**, uma conta de **comprador de teste** e meios de pagamento de teste.
- Não compartilhe tokens e **não faça commit de arquivos com credenciais**.
- Para habilitar webhooks, defina `MP_WEBHOOK_SECRET` e `MP_PUBLIC_URL` (URL pública HTTPS). A rota é `POST /api/pagamentos/webhook`.
- Sem URL pública, a tela de cobrança oferece **Verificar status**, que consulta o Mercado Pago a partir da referência criada pelo back-end.
- A URL de checkout é devolvida pelo Mercado Pago; **não** se recebem dados de cartão na aplicação Java.

## Rotas de apresentação

- `/home` — painel inicial
- `/usuarios`, `/recepcionistas`, `/pacientes`, `/dentistas`, `/procedimentos` — cadastros
- `/escalas` — escalas mensais e edição por dia
- `/admin/relatorios` — relatórios e CSV
- `/agenda` — agenda por data e disponibilidade
- `/consultas` — lista e filtro por período/status
- `/recepcao/andamento` — registro de chegada e progresso
- `/pagamentos` — cobranças e pagamentos presenciais
- `/atendimentos` — atendimentos realizados pelo dentista
- `/historico-pacientes` — prontuários anteriores
- `/paciente/consultas`, `/paciente/pagamentos`, `/paciente/meus-dados` — portal do paciente
- `/chat` — chat paciente ↔ recepção

As APIs JSON existentes seguem disponíveis (pagamento, chat, relatórios e andamento). As telas HTML usam o mesmo serviço e as mesmas regras de autorização das APIs.

## Verificação e apresentação

Consulte [ROTEIRO_DE_TESTES.md](ROTEIRO_DE_TESTES.md) para demonstrar o sistema, com resultados esperados e casos de rejeição de acesso. Os testes unitários existentes estão em `src/test/java`. A compilação completa e os testes de integração com PostgreSQL e Mercado Pago devem ser executados no seu computador.

## Limites de segurança antes de usar com pacientes reais

Este é um sistema **acadêmico demonstrável**, não um produto clínico homologado. Antes de um uso real com dados de saúde, seriam necessárias, entre outras medidas: autenticação/controle de sessão reforçados, proteção abrangente contra CSRF, HTTPS obrigatório, auditoria de acessos a prontuários, gestão de consentimento e retenção pela LGPD, testes de concorrência, backups/restauração e validação dos processos financeiros/estornos. Utilize **dados fictícios** para a apresentação. `ddl-auto=update` é conveniente no desenvolvimento; em produção deve ser substituído por migrações controladas.
