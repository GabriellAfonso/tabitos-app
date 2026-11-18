# Tarefas 032 · Remarcar tarefa

Caminhos Kotlin relativos a `app/src/{main,test,androidTest}/java/com/ethos/app/`, conforme a árvore indicada.

## Preparação

- [X] T001 Conferir specs 021/022/024 e a compatibilidade dos registros em `core/tasks/domain/TaskModels.kt`.

## Fundação

- [X] T002 Preservar os tipos existentes e acrescentar os dois tipos de auditoria em `core/tasks/domain/TaskModels.kt`.

## US1 · Escolher o prazo (P1)

- [X] T003 [P] [US1] Cobrir antecipação e correção em test `core/tasks/domain/TaskPostponeTest.kt` e os atalhos em test `features/tasks/RescheduleDatesTest.kt`.
- [X] T004 [US1] Aplicar a direção e validar o prazo em `core/tasks/domain/TaskRules.kt`.
- [X] T005 [US1] Montar as datas rápidas em `features/tasks/TaskDetailPresentation.kt` e conectar folha, seletor e feedback em `features/tasks/TaskDetail.kt` e `app/src/main/res/values/strings.xml`.

## US2 · Lembretes e desfazer (P1)

- [X] T006 [P] [US2] Cobrir deslocamento negativo, retorno e horário passado em test `core/reminders/ReminderPostponeTest.kt`.
- [X] T007 [US2] Conferir o deslocamento transacional de `core/tasks/data/RoomTaskRepository.kt` e impedir entrega retroativa em `core/reminders/domain/ReminderRules.kt`.
- [X] T008 [US2] Cobrir leitura após remarcação e desfazer em androidTest `core/tasks/data/ChangedRowsDatabaseTest.kt`.

## US3 · Histórico e Adiadas (P2)

- [X] T009 [P] [US3] Cobrir a direção, correção e contagem em test `features/statistics/StatisticsHistoryTest.kt`.
- [X] T010 [US3] Mapear os registros e rótulos em `features/statistics/StatisticsHistory.kt`, `StatisticsContent.kt` e recursos.

## Verificação

- [X] T011 Cobrir falha/repetição em test `features/tasks/TasksViewModelTest.kt` e a folha com fonte a 200% em androidTest `features/tasks/TasksContentTest.kt`. Rodar build debug, testes JVM e lint. Registrar resultados, revisão de acessibilidade e limites em `verification.md`.

## Dependências e entrega

T001–T002 fornecem os eventos compartilhados. O primeiro incremento é US1, verificável com sábado → quinta.
Os testes de US2 (horários e volta) e US3 (histórico e contagem) podem ser preparados independentemente dos
componentes visuais da US1. US2 e US3 usam os eventos de T004. T011 verifica os três incrementos reunidos.
