# Plano de implementação da feature 035

Branch de trabalho `master` · 2026-11-19 · [Especificação](spec.md)

## Abordagem

Acrescentar `DeadlineKind.UNTIL_DATE` ao fim do enum (gravado pelo nome, sem renomear os existentes). A data final
fica em `TaskFields.date` e o horário opcional em `TaskFields.minute`, como em Dia específico. O início escolhido
fica em `RepeatRule.start` da definição, campo já gravado para toda tarefa (para tarefas únicas hoje ele repete a
data). Ao gerar a ocorrência, `periodStart = min(rule.start, date)` e `dueDate = date`; assim Hoje, Revisar e
Calendário funcionam pela regra de período já existente (`inDay`, `needsReview`). Sem mudança de schema (7).

Remarcar reaproveita `moveDate`: só a data final muda e a regra da definição é preservada, então o início efetivo
volta sozinho ao desfazer. Lembretes padrão e o limite de horário passam a aceitar `UNTIL_DATE` junto de `DAY`.

## Contexto técnico e organização

Android, Kotlin, módulo `:app`, Compose Material 3, Hilt e Room 2.8.5 (schema 7, inalterado).

- Domínio `core/tasks/domain`: enum, `canReschedule`, validação (horário e não recorrência), `makeOccurrence`
  usando a regra atualizada na edição.
- Lembretes `core/reminders/domain/DefaultTaskReminders.kt` e `core/tasks/data/RoomTaskRepository.kt`: tipos com
  data final e horário (`DeadlineKind.hasDayTime`).
- Editor `features/tasks`: opção no seletor só para tarefa não recorrente, campos Início/Fim/Horário, resumo
  "Até 28 nov 18:00", Repetir indisponível enquanto o tipo estiver escolhido; regras puras em
  `TaskEditorPresentation.kt`; `saveDraft` grava o início em `rule.start` e rejeita fim antes do início.
- Estatísticas, Calendário, cartões e detalhes já leem `dueDate`/`minute`; sem mudança além do enum exaustivo.

## Constitution Check

Antes e após o desenho: FR-001 a FR-013 cobertos por domínio, editor e testes JVM. Domínio sem Android; Composable
só desenha, decisões em funções puras. Textos em `strings.xml`. Offline, sem permissões novas, sem logs de
conteúdo. Dados existentes preservados: nenhum tipo antigo muda de nome nem de regra; nenhuma migração. A
implementação foi solicitada expressamente pelo usuário (2026-11-19).

## Entrega

Consultar [decisões](research.md), [modelo](data-model.md), [contrato de interface](contracts/interface.md) e
[roteiro de verificação](quickstart.md).
