# Roteiro de verificação da feature 035

## JVM

`.\gradlew.bat :app:testDebugUnitTest --tests "com.ethos.app.features.tasks.*" --tests "com.ethos.app.core.tasks.*" --tests "com.ethos.app.core.reminders.*"`

Esperado: criar "Até uma data" de hoje a +9 dias aparece em `inDay` em todos os dias do período e em Revisar no
dia seguinte; horário aceito; regra recorrente rejeitada; Remarcar muda o fim e o desfazer restaura o período;
lembrete padrão 30 min antes do fim com horário; tipos antigos inalterados.

## Manual (celular, quando o usuário quiser)

1. Nova tarefa → Prazo → Até uma data → escolher fim em 28 nov → salvar: chip "Até 28 nov", tarefa no grupo
   semanal de Hoje.
2. Editar → Início 24 nov: some de Hoje até 24 nov.
3. Detalhes → Remarcar → Amanhã → Desfazer.
4. Prazo "Até uma data" escolhido: Repetir indisponível.
