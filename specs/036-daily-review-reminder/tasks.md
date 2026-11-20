# Tarefas · Lembrete de revisão

Caminhos Kotlin relativos a app/src/main/java/com/ethos/app.

## Preparação

- [X] T001 Validar o escopo e a preservação de dados em spec.md, plan.md e data-model.md.

## US1 · Entrega útil no horário

- [X] T002 [US1] Cobrir dias elegíveis, dias silenciosos e validade do alarme em app/src/test/java/com/ethos/app/core/reminders/ReminderRulesTest.kt.
- [X] T003 [US1] Implementar elegibilidade e horário em core/reminders/domain/ReminderRules.kt.
- [X] T004 [US1] Reservar a data e validar o alarme em core/reminders/data/RoomReminderRepository.kt e core/reminders/platform/ReminderCoordinator.kt.

## US2 · Painel e toque

- [X] T005 [US2] Implementar agendamento, texto, continuidade e expiração em core/reminders/platform/ReminderPlatform.kt.
- [X] T006 [US2] Abrir Hoje com filtro Tudo a partir da notificação em MainActivity.kt e EthosApp.kt.

## US3 · Configuração compreensível

- [X] T007 [P] [US3] Atualizar nome, descrição, seletor e cobertura de interface em features/reminders/ReminderSettings.kt, app/src/main/res/values/strings.xml e app/src/androidTest/java/com/ethos/app/ReminderUiTest.kt.

## Verificação

- [X] T008 Conferir correspondência entre histórias, plano e código, executar JVM e registrar resultados em verification.md.

T002 precede T003–T005. T006 usa o contrato de toque de T005. T007 pode ocorrer em paralelo com as regras.
O primeiro incremento útil é US1. US2 completa o ciclo de vida e US3 apresenta o ajuste.
