# Decisões da feature 035

## Onde guardar o início

- Decisão: `RepeatRule.start` da definição (coluna `start` de `task_definitions`).
- Motivo: já existe e é gravado para toda tarefa; tarefas únicas não usam esse valor em nenhuma regra, então
  nenhuma tarefa existente muda. Evita migração no banco da rotina real.
- Alternativa: novo campo em `TaskFields`/colunas `field_start` em definições e ocorrências; exigiria migração
  7→8 sem ganho para tarefas únicas.

## Início efetivo depois de Remarcar

- Decisão: `periodStart = min(rule.start, date)` calculado ao gerar a ocorrência.
- Motivo: Remarcar e seu Desfazer só mudam a data final; o início escolhido nunca é sobrescrito, então desfazer
  restaura o período anterior sem guardar estado extra.
- Alternativa: mover o início junto; perde a escolha original e complica o Desfazer.

## Repetição

- Decisão: rejeitar `UNTIL_DATE` com regra recorrente na validação do domínio; o editor não oferece o tipo para
  tarefa recorrente e desabilita Repetir com o tipo escolhido.
- Motivo: decisão do usuário; uma janela livre não tem significado definido em séries.

## Horário e lembretes

- Decisão: tratar `UNTIL_DATE` como `DAY` para horário e lembretes padrão (spec 033), relativos ao fim.
- Motivo: o fim é o prazo real; reaproveita regras e testes existentes.
