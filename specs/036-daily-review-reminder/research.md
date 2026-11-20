# Decisões técnicas

- Hoje já usa TaskOccurrence.inDay para selecionar tarefas. A prática usa a regra vigente, o intervalo ativo,
  os dias da semana e a presença de um registro não excluído nessa data.
- O antigo resumo era emitido em qualquer reconciliação após o horário. Identificar o disparo agendado evita que
  uma edição tardia ou reabertura gere o aviso. A separação do alarme normal preserva a recuperação dos demais lembretes.
- A entrega local usa o mesmo canal e tag. O prazo nativo de expiração está disponível a partir da API 26.
  A reconciliação na virada do dia também limpa avisos antigos nas versões anteriores.
- A dispensa por gesto com o aparelho desbloqueado é controlada pelo Android.
  [Comportamento no Android 14](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications).
