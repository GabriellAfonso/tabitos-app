# Verificação da feature 035

2026-11-19 · [Especificação](spec.md) · [Tarefas](tasks.md)

## Executado

`.\gradlew.bat :app:testDebugUnitTest --tests "com.ethos.app.features.tasks.*" --tests "com.ethos.app.core.tasks.*" --tests "com.ethos.app.core.reminders.*"`
— aprovado, 107 testes JVM, sem falhas. Novos: `UntilDateDeadlineTest` (6), `UntilDateEditorTest` (6) e
`DefaultTaskRemindersTest.untilDateWindowIsRemindedBeforeItsEnd`.

Cobertos: período do início ao fim em Hoje/Calendário (`inDay`), Revisar no dia seguinte ao fim, início posterior,
horário aceito, recorrência rejeitada, edição do início, Remarcar só o fim e Desfazer restaurando o período,
lembrete padrão 30 min antes do fim, tipos antigos inalterados, opções e limites do editor.

## Folha Prazo opção C (2026-11-19)

`.\gradlew.bat :app:testDebugUnitTest --tests "com.ethos.app.features.tasks.*" --tests "com.ethos.app.core.tasks.*"`
— aprovado. `UntilDateEditorTest` cobre início livre a partir de hoje, fim acompanhando o início, semana nova =
semana atual sem cartão, e abertura direta do calendário ao escolher "Até uma data". Visual não verificado.

## Não executado

- Build debug, lint e suíte JVM completa: não pedidos.
- Testes instrumentados e teste manual no celular (roteiro em [quickstart](quickstart.md)).

## Dados existentes

Sem mudança de schema (7). `DeadlineKind.UNTIL_DATE` acrescentado ao fim do enum; nomes gravados inalterados.
Tarefas existentes não leem `RepeatRule.start` como início de período, então mantêm o comportamento.
