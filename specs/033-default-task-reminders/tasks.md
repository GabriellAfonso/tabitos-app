# Tarefas da feature 033

Os caminhos Kotlin abaixo partem de `app/src/main/java/com/ethos/app/`.
Os diretórios test e androidTest seguem o mesmo pacote.

## Preparação e fundação

- [X] T001 Verificar os cenários da especificação em `specs/033-default-task-reminders/spec.md`.
- [X] T002 Testar preferência ausente, vazia e persistida em `core/preferences/TaskReminderDefaultsTest.kt` (test).
- [X] T003 Criar o contrato, a persistência e o vínculo em `core/preferences/TaskReminderDefaults.kt` e `PreferencesModule.kt`.

## US1 · Criar uma tarefa e receber o aviso de 30 minutos

- [X] T004 [P] [US1] Cobrir vencimento, novos prazos e recorrência em `core/reminders/DefaultTaskRemindersTest.kt` (test).
- [X] T005 [US1] Gerar avisos relativos e gravá-los na criação em `core/tasks/data/RoomTaskRepository.kt` e `TaskModule.kt`.
- [X] T006 [US1] Conectar criação com lembretes à permissão contextual em `features/tasks/TasksViewModel.kt` e `EthosApp.kt`.

## US2 · Escolher e persistir os padrões

- [X] T007 [P] [US2] Expor estado, salvamento e recuperação em `features/reminders/ReminderViewModel.kt`.
- [X] T008 [US2] Incluir opções e horário acessíveis em `features/reminders/ReminderSettings.kt` e `app/src/main/res/values/strings.xml`.

## US3 · Ajustar ou silenciar no sininho

- [X] T009 [P] [US3] Exibir a antecedência ou referência correta em `features/reminders/ReminderContent.kt`.
- [X] T010 [US3] Cobrir tarefas antigas, exceções e reabertura em `ReminderIntegrationTest.kt` (androidTest).

## Verificação

- [X] T011 Rodar a suíte JVM e registrar resultados e limites em `specs/033-default-task-reminders/verification.md`.

T001–T003 fornecem o contrato. Depois, US1 grava definições comuns, US2 controla novas criações e US3
exibe essas definições. Os testes de domínio, a integração do estado das configurações e a apresentação
do sininho podem avançar em paralelo. O roteiro independente de cada história está em [quickstart](quickstart.md).
