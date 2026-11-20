# Verificação dos lembretes padrão de tarefas

Em 2026-11-18, a suíte `:app:testDebugUnitTest` concluiu com **304 testes aprovados**, zero falhas,
erros ou testes ignorados.

- `TaskReminderDefaultsTest` reabriu o DataStore e distinguiu ausência de preferência, conjunto vazio,
  horário de dia e as duas antecedências.
- `DefaultTaskRemindersTest` verificou o aviso das 14h30 para prazo às 15h, remarcação e conclusão,
  antecedências vencidas na criação, regras para as próximas ocorrências, data sem hora e prazos por semana.
- A revisão comparou FR-001 a FR-007 com as tarefas T002 a T010. A aplicação do padrão ocorre na
  transação de criação. Edição, atualização do app e materialização de ocorrências dispensam essa etapa.
  O agendamento usa as mesmas definições de lembrete que o sininho edita. O schema Room continua na versão 7.
- Os testes instrumentados escritos em `ReminderIntegrationTest` cobrem reabertura, preservação de
  séries antigas, exceções, repetição de Create e rollback de uma falha de gravação. `ReminderUiTest`
  cobre a seleção das preferências e a apresentação do horário de referência.

O build `:app:assembleDebug` passou ao gerar o APK para instalação. Lint e testes instrumentados não executados.
A entrega de notificações no Android e a interação com TalkBack permanecem sem validação em dispositivo.
