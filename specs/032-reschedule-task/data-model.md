# Modelo de dados

- TaskOccurrence mantém id, série, ordinal e hora. Mudar o dia afeta a ocorrência escolhida. A data planejada
  da série continua a identificar a posição original de uma ocorrência recorrente.
- TaskAudit é persistido por nome. Acrescentar ADVANCED e ADVANCE_UNDONE após os valores existentes. Nos quatro
  tipos de remarcação/correção, originalDeadline é a data imediatamente anterior e actualDate é o destino.
- Remarcar para uma data posterior gera POSTPONED. Para uma anterior gera ADVANCED. Desfazer produz a correção
  correspondente à operação original. A partir de um prazo vencido também se registra MISSED_DEADLINE.
- ReminderDefinition conserva hora local e escopo. Uma data fixa de série pode receber uma cópia da ocorrência.
  Um aviso relativo deriva sua hora do prazo. Nenhuma nova coluna é necessária.
