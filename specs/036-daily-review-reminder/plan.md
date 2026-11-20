# Plano de implementação · Lembrete de revisão

19/11/2026 · [Especificação](spec.md)

## Resumo e contexto técnico

Alterar o fluxo de resumo em core/reminders e a linha de configuração Compose no módulo Android :app.
Usar a mesma preferência Room, canal de notificação e política de alarme exato ou aproximado.
A regra pura recebe data e snapshots reais. O alarme de revisão identifica o horário a que pertence,
independente das reconciliações causadas por edição de tarefas ou abertura do app.

## Constitution Check

Antes e depois do design: I — escopo aprovado na spec; II — operação local, esquema 7 e horário preservados;
III — decisão de elegibilidade no domínio; IV — recursos pt-BR, conteúdo neutro, controle acessível;
V — regressões JVM para elegibilidade e agendamento e roteiro Android para painel, navegação e ajuste.

## Estrutura e execução

1. Alterar ReminderSettings, textos e teste de UI.
2. Substituir contagens em ReminderRules por elegibilidade usando inDay, regras de prática e registros do dia.
3. Selecionar a próxima data civil, validar a identidade do alarme recebido e reservar a entrega em Room.
4. Expirar o aviso no painel, tratar o toque na Activity e navegar para Hoje.
5. Conferir o contrato, executar a suíte JVM filtrada e registrar limitações em verification.md.
