# Verificação · Remarcar tarefa

2026-11-18. Dados fictícios nos testes. Schema Room 7.

| Verificação | Resultado |
| --- | --- |
| APK debug | Gerado por `:app:assembleDebug` |
| Suíte JVM | 280 testes aprovados |
| Lint debug | 0 erros e 28 avisos |
| Fontes dos testes Android | Compiladas por `:app:compileDebugAndroidTestKotlin` |
| Android em execução | Testes instrumentados, TalkBack por áudio e inspeção visual em aparelho ou emulador não executados |

## Evidências por história

- US1. `RescheduleDatesTest` verifica os atalhos e a seleção inicial para prazos vencido, hoje, amanhã e
  futuro. `TaskPostponeTest` verifica validação de datas, horário, datas coincidentes na série, revisão de
  vencidas, correções e rejeição de um desfazer quando o prazo já mudou novamente.
- US2. `ReminderPostponeTest` verifica que dois dias de antecipação deslocam um aviso fixo das 09:00 e um
  aviso relativo de uma hora, que o momento já passado fica fora da agenda e que desfazer restaura a data.
  `TasksViewModelTest` cobre falha de gravação, repetição, fechamento dos detalhes e desfazer.
- US3. `StatisticsHistoryTest` verifica os dois novos tipos no filtro de tarefas, a data restaurada, o ajuste e
  o total de Adiadas. Também cobre que as moedas de uma conclusão próxima pertencem à conclusão.
- Na revisão do código, folha com rolagem, opções de pelo menos 48 dp, rótulo e data na linha acionável.
  Mensagem com timeout de oito segundos ajustado pelo sistema e cores do tema. Há cenários instrumentados
  para fonte a 200% e para leitura de prazo, auditoria e lembrete após salvar e desfazer. A validação desses
  cenários em Android permanece pendente.
