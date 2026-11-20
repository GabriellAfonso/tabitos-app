# Tarefas da feature 035 · Prazo "Até uma data"

Entrada: [spec](spec.md), [plano](plan.md), [modelo](data-model.md), [contrato](contracts/interface.md).
Testes JVM pedidos pela constituição para regras de negócio.

## Fase 1 · Fundação (bloqueia as histórias)

- [X] T001 Acrescentar `UNTIL_DATE` ao fim de `DeadlineKind` e a propriedade `hasDayTime` (DAY, UNTIL_DATE) em app/src/main/java/com/ethos/app/core/tasks/domain/TaskModels.kt
- [X] T002 Em app/src/main/java/com/ethos/app/core/tasks/domain/TaskRules.kt: validar horário só com `hasDayTime`, rejeitar `UNTIL_DATE` com regra recorrente, `makeOccurrence` com `periodStart = min(rule.start, date)` e `edit` gerando a ocorrência com a regra da ação

## Fase 2 · US1 Criar tarefa até uma data (P1)

- [X] T003 [US1] Teste JVM: criar de hoje a +9 dias aparece em `inDay` no período, em Revisar depois, recorrência rejeitada e tipos antigos inalterados em app/src/test/java/com/ethos/app/core/tasks/UntilDateDeadlineTest.kt
- [X] T004 [US1] Funções puras do editor (`deadlineKindsFor` sem UNTIL_DATE para recorrente, `withDeadline` definindo início hoje e limpando horário só fora de `hasDayTime`, `allowsRepeat`) em app/src/main/java/com/ethos/app/features/tasks/TaskEditorPresentation.kt
- [X] T005 [US1] `saveDraft` grava o início em `rule.start` para UNTIL_DATE, rejeita fim antes do início e guarda horário para `hasDayTime` em app/src/main/java/com/ethos/app/features/tasks/TasksViewModel.kt
- [X] T006 [US1] Opção "Até uma data", resumo "Até 28 nov 18:00" e Repetir indisponível em app/src/main/java/com/ethos/app/features/tasks/TaskEditor.kt e app/src/main/res/values/strings.xml
- [X] T007 [P] [US1] Teste JVM das funções puras do editor em app/src/test/java/com/ethos/app/features/tasks/UntilDateEditorTest.kt

## Fase 3 · US2 Horário e início (P2)

- [X] T008 [US2] Campos Início (máx. fim) e Data final (mín. início) e Horário no `WhenOptions` em app/src/main/java/com/ethos/app/features/tasks/TaskEditor.kt

## Fase 4 · US3 Integração (P3)

- [X] T009 [US3] `canReschedule` para UNTIL_DATE em app/src/main/java/com/ethos/app/core/tasks/domain/TaskModels.kt; teste Remarcar/Desfazer em app/src/test/java/com/ethos/app/core/tasks/UntilDateDeadlineTest.kt
- [X] T010 [US3] Lembretes padrão usam `hasDayTime` em app/src/main/java/com/ethos/app/core/reminders/domain/DefaultTaskReminders.kt e app/src/main/java/com/ethos/app/core/tasks/data/RoomTaskRepository.kt; teste em app/src/test/java/com/ethos/app/core/reminders/DefaultTaskRemindersTest.kt

## Fase 5 · Acabamento

- [X] T011 Rodar testes JVM filtrados (quickstart.md) e registrar em specs/035-deadline-until-date/verification.md
- [X] T012 Registrar a feature no bloco de contexto de AGENTS.md

## Dependências

T001→T002→(US1, US3). T004→T006/T008. US2 e US3 dependem só da fundação. MVP: Fases 1–2.
