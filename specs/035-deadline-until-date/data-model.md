# Modelo da feature 035

Sem mudança de schema (Room 7).

| Conceito | Onde fica | Regra |
|---|---|---|
| Tipo "Até uma data" | `DeadlineKind.UNTIL_DATE` (último do enum, gravado pelo nome) | Só em tarefa não recorrente |
| Data final | `TaskFields.date` (definição e ocorrência) | Obrigatória |
| Horário final | `TaskFields.minute` | Opcional, 0..1439 |
| Início escolhido | `RepeatRule.start` da definição (`kind = NONE`) | Padrão hoje; ≤ data final ao salvar |
| Período da ocorrência | `periodStart = min(início, data final)`, `dueDate = data final` | Calculado ao gerar/editar |

Transições: criar/editar recalcula o período; Remarcar muda só a data final (auditoria POSTPONED/ADVANCED);
Desfazer volta a data final e o período anterior; vencida vai para Revisar pela regra existente (`dueDate < hoje`).
